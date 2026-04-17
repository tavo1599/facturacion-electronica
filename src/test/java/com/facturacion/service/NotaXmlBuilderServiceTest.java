package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.NotaRequestDTO;
import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para NotaXmlBuilderService - XML UBL 2.1 para NC y ND
 * Verifica CreditNote, DebitNote, BillingReference, DiscrepancyResponse
 */
class NotaXmlBuilderServiceTest {

    private NotaXmlBuilderService service;
    private XmlUtilService xmlUtil;

    private static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String NS_CREDIT = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    private static final String NS_DEBIT = "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";

    @BeforeEach
    void setUp() {
        service = new NotaXmlBuilderService();
        xmlUtil = new XmlUtilService();
    }

    // ==================== NOTAS DE CRÉDITO ====================

    @Nested
    @DisplayName("NC Gravada - XML")
    class NCGravadaXml {

        private Document doc;

        @BeforeEach
        void build() throws Exception {
            doc = service.buildXml(TestDataBuilder.notaCreditoGravada());
        }

        @Test
        @DisplayName("Raíz es CreditNote")
        void raiz() {
            assertEquals("CreditNote", doc.getDocumentElement().getLocalName());
            assertEquals(NS_CREDIT, doc.getDocumentElement().getNamespaceURI());
        }

        @Test
        @DisplayName("Contiene DiscrepancyResponse")
        void discrepancy() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "DiscrepancyResponse");
            assertEquals(1, nodes.getLength());
        }

        @Test
        @DisplayName("Contiene BillingReference")
        void billingRef() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "BillingReference");
            assertEquals(1, nodes.getLength());
        }

        @Test
        @DisplayName("Usa CreditNoteLine")
        void creditNoteLine() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "CreditNoteLine");
            assertTrue(nodes.getLength() > 0);
        }

        @Test
        @DisplayName("Contiene tributo IGV 1000")
        void tributoIgv() throws Exception {
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("1000"));
            assertTrue(xml.contains("IGV"));
        }

        @Test
        @DisplayName("Usa RequestedMonetaryTotal")
        void monetaryTotal() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "RequestedMonetaryTotal");
            assertEquals(1, nodes.getLength());
        }
    }

    @Nested
    @DisplayName("NC Exonerada - XML")
    class NCExoneradaXml {

        @Test
        @DisplayName("Contiene tributo exonerado 9997")
        void tributo() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaCreditoExonerada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9997"));
        }
    }

    @Nested
    @DisplayName("NC Inafecta - XML")
    class NCInafectaXml {

        @Test
        @DisplayName("Contiene tributo inafecto 9998")
        void tributo() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaCreditoInafecta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9998"));
        }
    }

    @Nested
    @DisplayName("NC Mixta - XML")
    class NCMixtaXml {

        @Test
        @DisplayName("Contiene los tres tributos")
        void tresTributos() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaCreditoMixta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("1000"));
            assertTrue(xml.contains("9997"));
            assertTrue(xml.contains("9998"));
        }

        @Test
        @DisplayName("Tiene 3 CreditNoteLines")
        void tresLineas() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaCreditoMixta());
            NodeList lines = doc.getElementsByTagNameNS(NS_CAC, "CreditNoteLine");
            assertEquals(3, lines.getLength());
        }
    }

    @Nested
    @DisplayName("NC sobre Boleta - XML")
    class NCSobreBoletaXml {

        @Test
        @DisplayName("Serie B en ID")
        void serieB() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaCreditoSobreBoleta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("B001-00000005"));
        }
    }

    // ==================== NOTAS DE DÉBITO ====================

    @Nested
    @DisplayName("ND Gravada - XML")
    class NDGravadaXml {

        private Document doc;

        @BeforeEach
        void build() throws Exception {
            doc = service.buildXml(TestDataBuilder.notaDebitoGravada());
        }

        @Test
        @DisplayName("Raíz es DebitNote")
        void raiz() {
            assertEquals("DebitNote", doc.getDocumentElement().getLocalName());
            assertEquals(NS_DEBIT, doc.getDocumentElement().getNamespaceURI());
        }

        @Test
        @DisplayName("Usa DebitNoteLine")
        void debitNoteLine() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "DebitNoteLine");
            assertTrue(nodes.getLength() > 0);
        }

        @Test
        @DisplayName("Contiene DiscrepancyResponse")
        void discrepancy() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "DiscrepancyResponse");
            assertEquals(1, nodes.getLength());
        }

        @Test
        @DisplayName("Contiene BillingReference")
        void billingRef() {
            NodeList nodes = doc.getElementsByTagNameNS(NS_CAC, "BillingReference");
            assertEquals(1, nodes.getLength());
        }
    }

    @Nested
    @DisplayName("ND Exonerada - XML")
    class NDExoneradaXml {

        @Test
        @DisplayName("Contiene tributo exonerado")
        void tributo() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaDebitoExonerada());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9997"));
        }
    }

    @Nested
    @DisplayName("ND Inafecta - XML")
    class NDInafectaXml {

        @Test
        @DisplayName("Contiene tributo inafecto")
        void tributo() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaDebitoInafecta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("9998"));
        }
    }

    @Nested
    @DisplayName("ND Mixta - XML")
    class NDMixtaXml {

        @Test
        @DisplayName("Tres tributos y tres líneas")
        void tresTributos() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaDebitoMixta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("1000"));
            assertTrue(xml.contains("9997"));
            assertTrue(xml.contains("9998"));
            NodeList lines = doc.getElementsByTagNameNS(NS_CAC, "DebitNoteLine");
            assertEquals(3, lines.getLength());
        }
    }

    @Nested
    @DisplayName("ND sobre Boleta - XML")
    class NDSobreBoletaXml {

        @Test
        @DisplayName("Serie B")
        void serieB() throws Exception {
            Document doc = service.buildXml(TestDataBuilder.notaDebitoSobreBoleta());
            String xml = xmlUtil.documentToString(doc);
            assertTrue(xml.contains("B001-00000005"));
        }
    }

    // ==================== HELPERS ====================
}

