package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para XmlBuilderService - genera XML UBL 2.1 para Facturas y Boletas
 * Verifica estructura XML con todos los tipos de afectación IGV
 */
class XmlBuilderServiceTest {

    private XmlBuilderService service;
    private XmlUtilService xmlUtil;

    private static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";

    @BeforeEach
    void setUp() {
        service = new XmlBuilderService();
        xmlUtil = new XmlUtilService();
    }

    // ==================== FACTURA GRAVADA ====================

    @Nested
    @DisplayName("Factura Gravada - XML")
    class FacturaGravadaXml {

        private Document doc;

        @BeforeEach
        void build() throws Exception {
            doc = service.buildXml(TestDataBuilder.facturaGravada());
        }

        @Test
        @DisplayName("Raíz es Invoice")
        void raizInvoice() {
            assertEquals("Invoice", doc.getDocumentElement().getLocalName());
            assertEquals(NS_INVOICE, doc.getDocumentElement().getNamespaceURI());
        }

        @Test
        @DisplayName("UBLVersionID = 2.1")
        void ublVersion() {
            assertEquals("2.1", getCbcValue(doc, "UBLVersionID"));
        }

        @Test
        @DisplayName("ID correcto")
        void id() {
            assertEquals("F001-00000001", getCbcValue(doc, "ID"));
        }

        @Test
        @DisplayName("InvoiceTypeCode = 01")
        void tipoComprobante() {
            assertEquals("01", getCbcValue(doc, "InvoiceTypeCode"));
        }

        @Test
        @DisplayName("Fecha de emisión correcta")
        void fecha() {
            assertEquals("2025-04-16", getCbcValue(doc, "IssueDate"));
        }

        @Test
        @DisplayName("Moneda = PEN")
        void moneda() {
            assertEquals("PEN", getCbcValue(doc, "DocumentCurrencyCode"));
        }

        @Test
        @DisplayName("Tiene UBLExtensions para firma")
        void extensionsFirma() {
            NodeList exts = doc.getElementsByTagNameNS(NS_EXT, "ExtensionContent");
            assertTrue(exts.getLength() > 0);
        }

        @Test
        @DisplayName("Tiene 2 InvoiceLines")
        void lineas() {
            NodeList lines = doc.getElementsByTagNameNS(NS_CAC, "InvoiceLine");
            assertEquals(2, lines.getLength());
        }

        @Test
        @DisplayName("TaxTotal contiene IGV gravado")
        void taxTotal() {
            NodeList taxTotals = doc.getElementsByTagNameNS(NS_CAC, "TaxTotal");
            assertTrue(taxTotals.getLength() > 0);
        }

        @Test
        @DisplayName("XML serializable sin errores")
        void serializable() throws Exception {
            byte[] bytes = xmlUtil.documentToBytes(doc);
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);
            String xml = new String(bytes, "UTF-8");
            assertTrue(xml.contains("Invoice"));
            assertTrue(xml.contains("F001-00000001"));
        }
    }

    // ==================== FACTURA EXONERADA ====================

    @Nested
    @DisplayName("Factura Exonerada - XML")
    class FacturaExoneradaXml {

        @Test
        @DisplayName("XML generado con tributo exonerado 9997")
        void tributoExonerado() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaExonerada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9997")); // Tributo exonerado
            assertTrue(xml.contains("EXO"));
        }
    }

    // ==================== FACTURA INAFECTA ====================

    @Nested
    @DisplayName("Factura Inafecta - XML")
    class FacturaInafectaXml {

        @Test
        @DisplayName("XML generado con tributo inafecto 9998")
        void tributoInafecto() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaInafecta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9998")); // Tributo inafecto
            assertTrue(xml.contains("INA"));
        }
    }

    // ==================== FACTURA MIXTA ====================

    @Nested
    @DisplayName("Factura Mixta - XML")
    class FacturaMixtaXml {

        @Test
        @DisplayName("XML contiene los tres tributos")
        void tresTributos() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaMixta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("1000")); // IGV
            assertTrue(xml.contains("9997")); // Exonerado
            assertTrue(xml.contains("9998")); // Inafecto
        }

        @Test
        @DisplayName("Tiene 3 InvoiceLines")
        void tresLineas() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaMixta());
            NodeList lines = doc.getElementsByTagNameNS(NS_CAC, "InvoiceLine");
            assertEquals(3, lines.getLength());
        }
    }

    // ==================== FACTURA USD ====================

    @Nested
    @DisplayName("Factura USD - XML")
    class FacturaUSDXml {

        @Test
        @DisplayName("Moneda USD en XML")
        void monedaUsd() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravadaUSD());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("USD"));
        }
    }

    // ==================== BOLETAS ====================

    @Nested
    @DisplayName("Boleta Gravada - XML")
    class BoletaGravadaXml {

        @Test
        @DisplayName("InvoiceTypeCode = 03")
        void tipoBoleta() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.boletaGravada());
            assertEquals("03", getCbcValue(doc, "InvoiceTypeCode"));
        }

        @Test
        @DisplayName("Serie empieza con B")
        void serie() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.boletaGravada());
            assertTrue(getCbcValue(doc, "ID").startsWith("B"));
        }
    }

    @Nested
    @DisplayName("Boleta Exonerada - XML")
    class BoletaExoneradaXml {

        @Test
        @DisplayName("Contiene tributo exonerado")
        void exonerado() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.boletaExonerada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9997"));
        }
    }

    @Nested
    @DisplayName("Boleta Inafecta - XML")
    class BoletaInafectaXml {

        @Test
        @DisplayName("Contiene tributo inafecto")
        void inafecto() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.boletaInafecta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9998"));
        }
    }

    @Nested
    @DisplayName("Boleta Mixta - XML")
    class BoletaMixtaXml {

        @Test
        @DisplayName("Contiene los tres tributos")
        void tresTributos() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.boletaMixta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("1000"));
            assertTrue(xml.contains("9997"));
            assertTrue(xml.contains("9998"));
        }
    }

    // ==================== ESTRUCTURA COMÚN ====================

    @Nested
    @DisplayName("Estructura XML común")
    class EstructuraComun {

        @Test
        @DisplayName("Contiene SupplierParty con RUC")
        void supplierParty() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("20123456789"));
            assertTrue(xml.contains("MI EMPRESA S.A.C."));
        }

        @Test
        @DisplayName("Contiene CustomerParty")
        void customerParty() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("20512345678"));
            assertTrue(xml.contains("EMPRESA CLIENTE S.A.C."));
        }

        @Test
        @DisplayName("Contiene Signature reference")
        void signature() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("IDSignSunat"));
        }

        @Test
        @DisplayName("Contiene PaymentTerms")
        void paymentTerms() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("FormaPago"));
            assertTrue(xml.contains("Contado"));
        }

        @Test
        @DisplayName("Contiene LegalMonetaryTotal")
        void monetaryTotal() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravada());
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "LegalMonetaryTotal");
            assertEquals(1, nodes.getLength());
        }

        @Test
        @DisplayName("Contiene leyenda monto en letras")
        void leyenda() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.facturaGravada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("SOLES"));
        }
    }

    // ==================== HELPERS ====================

    private String getCbcValue(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS(NS_CBC, localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
}

