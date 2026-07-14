package com.facturacion.service;

import com.facturacion.dto.ComprobanteLineaDTO;
import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.util.MontoEnLetras;
import com.facturacion.util.UblConstants;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;
import com.facturacion.util.UblConstants;

/**
 * Genera el XML UBL 2.1 para Facturas y Boletas según las guías de SUNAT
 */
@Service
public class XmlBuilderService {


    /**
     * Genera el documento XML UBL 2.1 completo
     */
    public Document buildXml(ComprobanteRequestDTO comprobante) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        // === ELEMENTO RAÍZ: <Invoice> ===
        // Solo los namespaces que realmente se usan (como en XML funcional SUNAT)
        Element root = doc.createElementNS(NS_INVOICE, "Invoice");
        root.setAttribute("xmlns", NS_INVOICE);
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:ds", NS_DS);
        doc.appendChild(root);

        // 1. UBLExtensions (contenedor para firma digital)
        buildUblExtensions(doc, root);

        // 2. UBLVersionID
        addCbcElement(doc, root, "UBLVersionID", UBL_VERSION);

        // 3. CustomizationID
        addCbcElement(doc, root, "CustomizationID", CUSTOMIZATION_ID);

        // 4. ID - Serie y correlativo
        addCbcElement(doc, root, "ID", comprobante.getSerieCorrelativo());

        // 5. IssueDate - Fecha de emisión
        LocalDate fecha = comprobante.getFechaEmision() != null 
            ? comprobante.getFechaEmision() : LocalDate.now();
        addCbcElement(doc, root, "IssueDate", fecha.format(DateTimeFormatter.ISO_DATE));

        // 6. IssueTime - Hora de emisión
        String hora = comprobante.getHoraEmision() != null 
            ? comprobante.getHoraEmision() 
            : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        addCbcElement(doc, root, "IssueTime", hora);

        // 7. DueDate - Fecha de vencimiento
        LocalDate fechaVenc = comprobante.getFechaVencimiento() != null
            ? comprobante.getFechaVencimiento() : fecha;
        addCbcElement(doc, root, "DueDate", fechaVenc.format(DateTimeFormatter.ISO_DATE));

        // 8. InvoiceTypeCode - Tipo de comprobante (Catálogo 01)
        Element typeCode = addCbcElement(doc, root, "InvoiceTypeCode", comprobante.getTipoComprobante());
        typeCode.setAttribute("listID", comprobante.getTipoOperacion());

        // 9. Note - Leyenda (monto en letras)
        // 9. Note - Leyenda (monto en letras)
        Element note = addCbcElement(doc, root, "Note",
                MontoEnLetras.convertir(comprobante.getImporteTotal(), comprobante.getMoneda()));
        note.setAttribute("languageLocaleID", "1000");

        // 9.1 Nota SPOT (Sistema de Pago de Obligaciones Tributarias) - solo si hay detracción
        if (Boolean.TRUE.equals(comprobante.getTieneDetraccion())) {
            Element noteSpot = addCbcElement(doc, root, "Note",
                    "Operación sujeta al Sistema de Pago de Obligaciones Tributarias");
            noteSpot.setAttribute("languageLocaleID", "2006");
        }

        // 10. DocumentCurrencyCode - Moneda
        addCbcElement(doc, root, "DocumentCurrencyCode", comprobante.getMoneda());


        // 11. Signature (referencia a la firma)
        EmpresaDTO empresa = comprobante.getEmpresa();
        buildSignatureReference(doc, root, empresa);

        // 12. AccountingSupplierParty (datos del emisor)
        buildSupplierParty(doc, root, empresa);

       // 13. AccountingCustomerParty (datos del cliente)
        buildCustomerParty(doc, root, comprobante);

        // 13.1 Detracción SUNAT (SOLO si aplica) - antes de PaymentTerms
        if (Boolean.TRUE.equals(comprobante.getTieneDetraccion())) {
            buildPaymentMeansDetraccion(doc, root, comprobante);
            buildPaymentTermsDetraccion(doc, root, comprobante);
        }

        // 14. PaymentTerms (forma de pago)
        buildPaymentTerms(doc, root, comprobante);

        // 15. TaxTotal (impuestos totales)
        buildTaxTotal(doc, root, comprobante);

        // 16. LegalMonetaryTotal (totales monetarios)
        buildLegalMonetaryTotal(doc, root, comprobante);

        // 17. InvoiceLines (líneas de detalle)
        for (ComprobanteLineaDTO linea : comprobante.getItems()) {
            buildInvoiceLine(doc, root, linea, comprobante.getMoneda());
        }

        return doc;
    }

    // ==================== SECCIONES DEL XML ====================

    /**
     * UBLExtensions - Contenedor para la firma digital
     */
    private void buildUblExtensions(Document doc, Element root) {
        Element extensions = doc.createElementNS(NS_EXT, "ext:UBLExtensions");
        Element extension = doc.createElementNS(NS_EXT, "ext:UBLExtension");
        Element content = doc.createElementNS(NS_EXT, "ext:ExtensionContent");
        // Se deja vacío; la firma se insertará aquí después
        extension.appendChild(content);
        extensions.appendChild(extension);
        root.appendChild(extensions);
    }

    /**
     * Signature - Referencia a la firma digital del emisor
     */
    private void buildSignatureReference(Document doc, Element root, EmpresaDTO empresa) {
        Element signature = doc.createElementNS(NS_CAC, "cac:Signature");

        addCbcElement(doc, signature, "ID", "IDSignSunat");

        Element signatoryParty = doc.createElementNS(NS_CAC, "cac:SignatoryParty");
        Element partyIdentification = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        addCbcElement(doc, partyIdentification, "ID", empresa.getRuc());
        signatoryParty.appendChild(partyIdentification);

        Element partyName = doc.createElementNS(NS_CAC, "cac:PartyName");
        addCbcElement(doc, partyName, "Name", empresa.getRazonSocial());
        signatoryParty.appendChild(partyName);

        signature.appendChild(signatoryParty);

        Element digitalAttachment = doc.createElementNS(NS_CAC, "cac:DigitalSignatureAttachment");
        Element externalRef = doc.createElementNS(NS_CAC, "cac:ExternalReference");
        addCbcElement(doc, externalRef, "URI", "#IDSignSunat");
        digitalAttachment.appendChild(externalRef);
        signature.appendChild(digitalAttachment);

        root.appendChild(signature);
    }

    /**
     * AccountingSupplierParty - Datos del emisor
     */
    private void buildSupplierParty(Document doc, Element root, EmpresaDTO empresa) {
        Element supplier = doc.createElementNS(NS_CAC, "cac:AccountingSupplierParty");
        Element party = doc.createElementNS(NS_CAC, "cac:Party");

        // PartyIdentification - RUC del emisor
        Element partyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element id = addCbcElement(doc, partyId, "ID", empresa.getRuc());
        id.setAttribute("schemeID", DOC_RUC);
        party.appendChild(partyId);

        // PartyName - Nombre comercial
        Element partyName = doc.createElementNS(NS_CAC, "cac:PartyName");
        addCbcElement(doc, partyName, "Name", empresa.getNombreComercialEffective());
        party.appendChild(partyName);

        // PartyLegalEntity - Razón social y dirección
        Element legalEntity = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, legalEntity, "RegistrationName", empresa.getRazonSocial());

        // Dirección del emisor
        Element address = doc.createElementNS(NS_CAC, "cac:RegistrationAddress");
        addCbcElement(doc, address, "ID", empresa.getUbigeo());
        addCbcElement(doc, address, "AddressTypeCode", "0000");

        Element citySubdivision = addCbcElement(doc, address, "CitySubdivisionName", "-");
        Element cityName = addCbcElement(doc, address, "CityName", empresa.getProvincia());
        Element countrySubentity = addCbcElement(doc, address, "CountrySubentity", empresa.getDepartamento());
        Element district = addCbcElement(doc, address, "District", empresa.getDistrito());

        // AddressLine
        Element addressLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
        addCbcElement(doc, addressLine, "Line", empresa.getDireccion());
        address.appendChild(addressLine);

        // Country
        Element country = doc.createElementNS(NS_CAC, "cac:Country");
        addCbcElement(doc, country, "IdentificationCode", empresa.getCodigoPais());
        address.appendChild(country);

        legalEntity.appendChild(address);
        party.appendChild(legalEntity);
        supplier.appendChild(party);
        root.appendChild(supplier);
    }

    /**
     * AccountingCustomerParty - Datos del cliente/receptor
     */
    private void buildCustomerParty(Document doc, Element root, ComprobanteRequestDTO comprobante) {
        Element customer = doc.createElementNS(NS_CAC, "cac:AccountingCustomerParty");
        Element party = doc.createElementNS(NS_CAC, "cac:Party");

        // PartyIdentification
        Element partyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element id = addCbcElement(doc, partyId, "ID", comprobante.getClienteNumeroDocumento());
        id.setAttribute("schemeID", comprobante.getClienteTipoDocumento());
        party.appendChild(partyId);

        // PartyLegalEntity
        Element legalEntity = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, legalEntity, "RegistrationName", comprobante.getClienteRazonSocial());

        // Dirección del cliente (si existe)
        if (comprobante.getClienteDireccion() != null && !comprobante.getClienteDireccion().isEmpty()) {
            Element address = doc.createElementNS(NS_CAC, "cac:RegistrationAddress");
            Element addressLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
            addCbcElement(doc, addressLine, "Line", comprobante.getClienteDireccion());
            address.appendChild(addressLine);
            legalEntity.appendChild(address);
        }

        party.appendChild(legalEntity);
        customer.appendChild(party);
        root.appendChild(customer);
    }

    /**
     * PaymentTerms - Forma de pago
     */
    private void buildPaymentTerms(Document doc, Element root, ComprobanteRequestDTO comprobante) {
        Element paymentTerms = doc.createElementNS(NS_CAC, "cac:PaymentTerms");
        addCbcElement(doc, paymentTerms, "ID", "FormaPago");
        addCbcElement(doc, paymentTerms, "PaymentMeansID", comprobante.getFormaPago());

        if ("Credito".equalsIgnoreCase(comprobante.getFormaPago())) {
            Element amount = addCbcElement(doc, paymentTerms, "Amount", 
                comprobante.getImporteTotal().toPlainString());
            amount.setAttribute("currencyID", comprobante.getMoneda());
        }

        root.appendChild(paymentTerms);
    }

    /**
     * TaxTotal - Totales de impuestos
     */
    private void buildTaxTotal(Document doc, Element root, ComprobanteRequestDTO comprobante) {
        Element taxTotal = doc.createElementNS(NS_CAC, "cac:TaxTotal");

        // Monto total de impuestos
        Element taxAmount = addCbcElement(doc, taxTotal, "TaxAmount", 
            comprobante.getTotalIgv().toPlainString());
        taxAmount.setAttribute("currencyID", comprobante.getMoneda());

        // Subtotal por tipo de tributo

        // --- IGV (Gravado) ---
        if (comprobante.getTotalGravado().compareTo(BigDecimal.ZERO) > 0) {
            buildTaxSubtotal(doc, taxTotal, comprobante.getMoneda(),
                    comprobante.getTotalGravado(),
                    comprobante.getTotalIgv(),
                    TRIBUTO_IGV_ID, TRIBUTO_IGV_NOMBRE, TRIBUTO_IGV_CODIGO);
        }

        // --- Exonerado ---
        if (comprobante.getTotalExonerado().compareTo(BigDecimal.ZERO) > 0) {
            buildTaxSubtotal(doc, taxTotal, comprobante.getMoneda(),
                    comprobante.getTotalExonerado(),
                    BigDecimal.ZERO,
                    TRIBUTO_EXONERADO_ID, TRIBUTO_EXONERADO_NOMBRE, TRIBUTO_EXONERADO_CODIGO);
        }

        // --- Inafecto ---
        if (comprobante.getTotalInafecto().compareTo(BigDecimal.ZERO) > 0) {
            buildTaxSubtotal(doc, taxTotal, comprobante.getMoneda(),
                    comprobante.getTotalInafecto(),
                    BigDecimal.ZERO,
                    TRIBUTO_INAFECTO_ID, TRIBUTO_INAFECTO_NOMBRE, TRIBUTO_INAFECTO_CODIGO);
        }

        root.appendChild(taxTotal);
    }

    /**
     * TaxSubtotal individual
     */
    private void buildTaxSubtotal(Document doc, Element parent, String moneda,
                                   BigDecimal baseImponible, BigDecimal montoImpuesto,
                                   String tributoId, String tributoNombre, String tributoCodigo) {

        Element taxSubtotal = doc.createElementNS(NS_CAC, "cac:TaxSubtotal");

        Element taxableAmount = addCbcElement(doc, taxSubtotal, "TaxableAmount", baseImponible.toPlainString());
        taxableAmount.setAttribute("currencyID", moneda);

        Element taxAmt = addCbcElement(doc, taxSubtotal, "TaxAmount", montoImpuesto.toPlainString());
        taxAmt.setAttribute("currencyID", moneda);

        Element taxCategory = doc.createElementNS(NS_CAC, "cac:TaxCategory");

        Element taxScheme = doc.createElementNS(NS_CAC, "cac:TaxScheme");
        addCbcElement(doc, taxScheme, "ID", tributoId);
        addCbcElement(doc, taxScheme, "Name", tributoNombre);
        addCbcElement(doc, taxScheme, "TaxTypeCode", tributoCodigo);

        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);
        parent.appendChild(taxSubtotal);
    }

    /**
     * LegalMonetaryTotal - Totales del comprobante
     */
    private void buildLegalMonetaryTotal(Document doc, Element root, ComprobanteRequestDTO comprobante) {
        Element monetaryTotal = doc.createElementNS(NS_CAC, "cac:LegalMonetaryTotal");
        String moneda = comprobante.getMoneda();

        // LineExtensionAmount (total valor venta sin impuestos)
        Element lineExtension = addCbcElement(doc, monetaryTotal, "LineExtensionAmount",
                comprobante.getTotalValorVenta().toPlainString());
        lineExtension.setAttribute("currencyID", moneda);

        // TaxInclusiveAmount (total con impuestos)
        Element taxInclusive = addCbcElement(doc, monetaryTotal, "TaxInclusiveAmount",
                comprobante.getImporteTotal().toPlainString());
        taxInclusive.setAttribute("currencyID", moneda);

        // PayableAmount (monto a pagar)
        Element payable = addCbcElement(doc, monetaryTotal, "PayableAmount",
                comprobante.getImporteTotal().toPlainString());
        payable.setAttribute("currencyID", moneda);

        root.appendChild(monetaryTotal);
    }

    /**
     * InvoiceLine - Línea de detalle individual
     */
    private void buildInvoiceLine(Document doc, Element root, ComprobanteLineaDTO linea, String moneda) {
        Element invoiceLine = doc.createElementNS(NS_CAC, "cac:InvoiceLine");

        // ID - número de línea
        addCbcElement(doc, invoiceLine, "ID", String.valueOf(linea.getNumero()));

        // InvoicedQuantity - cantidad
        Element quantity = addCbcElement(doc, invoiceLine, "InvoicedQuantity",
                linea.getCantidad().toPlainString());
        quantity.setAttribute("unitCode", linea.getUnidadMedida());

        // LineExtensionAmount - valor venta de la línea
        Element lineAmount = addCbcElement(doc, invoiceLine, "LineExtensionAmount",
                linea.getValorVenta().toPlainString());
        lineAmount.setAttribute("currencyID", moneda);

        // PricingReference - precio unitario con IGV
        Element pricingRef = doc.createElementNS(NS_CAC, "cac:PricingReference");
        Element altCondPrice = doc.createElementNS(NS_CAC, "cac:AlternativeConditionPrice");
        Element priceAmount = addCbcElement(doc, altCondPrice, "PriceAmount",
                linea.getPrecioUnitario().toPlainString());
        priceAmount.setAttribute("currencyID", moneda);

        Element priceTypeCode = addCbcElement(doc, altCondPrice, "PriceTypeCode", linea.getTipoPrecio());

        pricingRef.appendChild(altCondPrice);
        invoiceLine.appendChild(pricingRef);

        // TaxTotal de la línea
        buildLineTaxTotal(doc, invoiceLine, linea, moneda);

        // Item - descripción del producto
        Element item = doc.createElementNS(NS_CAC, "cac:Item");
        addCbcElement(doc, item, "Description", linea.getDescripcion());

        // SellersItemIdentification - código del producto
        Element sellersId = doc.createElementNS(NS_CAC, "cac:SellersItemIdentification");
        addCbcElement(doc, sellersId, "ID", linea.getCodigoProducto());
        item.appendChild(sellersId);

        invoiceLine.appendChild(item);

        // Price - valor unitario sin IGV
        Element price = doc.createElementNS(NS_CAC, "cac:Price");
        Element pAmount = addCbcElement(doc, price, "PriceAmount",
                linea.getValorUnitario().toPlainString());
        pAmount.setAttribute("currencyID", moneda);
        invoiceLine.appendChild(price);

        root.appendChild(invoiceLine);
    }

    /**
     * TaxTotal de una línea individual
     */
    private void buildLineTaxTotal(Document doc, Element parent, ComprobanteLineaDTO linea, String moneda) {
        Element taxTotal = doc.createElementNS(NS_CAC, "cac:TaxTotal");

        Element taxAmount = addCbcElement(doc, taxTotal, "TaxAmount",
                linea.getMontoIgv().toPlainString());
        taxAmount.setAttribute("currencyID", moneda);

        Element taxSubtotal = doc.createElementNS(NS_CAC, "cac:TaxSubtotal");

        Element taxableAmount = addCbcElement(doc, taxSubtotal, "TaxableAmount",
                linea.getValorVenta().toPlainString());
        taxableAmount.setAttribute("currencyID", moneda);

        Element taxAmt = addCbcElement(doc, taxSubtotal, "TaxAmount",
                linea.getMontoIgv().toPlainString());
        taxAmt.setAttribute("currencyID", moneda);

        // TaxCategory
        Element taxCategory = doc.createElementNS(NS_CAC, "cac:TaxCategory");

        // Porcentaje
        addCbcElement(doc, taxCategory, "Percent", linea.getPorcentajeIgv().toPlainString());

        // Tipo de afectación IGV (Catálogo 07)
        addCbcElement(doc, taxCategory, "TaxExemptionReasonCode", linea.getTipoAfectacionIgv());

        // TaxScheme
        Element taxScheme = doc.createElementNS(NS_CAC, "cac:TaxScheme");

        String tributoId, tributoNombre, tributoCodigo;
        switch (linea.getTipoAfectacionIgv()) {
            case "20": case "21":
                tributoId = TRIBUTO_EXONERADO_ID;
                tributoNombre = TRIBUTO_EXONERADO_NOMBRE;
                tributoCodigo = TRIBUTO_EXONERADO_CODIGO;
                break;
            case "30": case "31": case "32": case "33": case "34": case "35": case "36":
                tributoId = TRIBUTO_INAFECTO_ID;
                tributoNombre = TRIBUTO_INAFECTO_NOMBRE;
                tributoCodigo = TRIBUTO_INAFECTO_CODIGO;
                break;
            default: // "10"-"17" - Gravado
                tributoId = TRIBUTO_IGV_ID;
                tributoNombre = TRIBUTO_IGV_NOMBRE;
                tributoCodigo = TRIBUTO_IGV_CODIGO;
                break;
        }

        addCbcElement(doc, taxScheme, "ID", tributoId);
        addCbcElement(doc, taxScheme, "Name", tributoNombre);
        addCbcElement(doc, taxScheme, "TaxTypeCode", tributoCodigo);

        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);
        taxTotal.appendChild(taxSubtotal);
        parent.appendChild(taxTotal);
    }

    // ==================== UTILIDADES ====================

    /**
     * Agrega un elemento cbc:XXX al padre
     */
    private Element addCbcElement(Document doc, Element parent, String localName, String value) {
        Element element = doc.createElementNS(NS_CBC, "cbc:" + localName);
        if (value != null) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
        return element;
    }

    // ============================================================
    // MÉTODOS NUEVOS: DETRACCIÓN SUNAT
    // ============================================================

    /**
     * Genera el bloque PaymentMeans para detracción.
     * Contiene el número de cuenta del Banco de la Nación del emisor.
     */
    private void buildPaymentMeansDetraccion(Document doc, Element root,
                                              ComprobanteRequestDTO comprobante) {
        EmpresaDTO empresa = comprobante.getEmpresa();

        if (empresa.getCuentaDetraccionBN() == null || empresa.getCuentaDetraccionBN().isBlank()) {
            throw new IllegalArgumentException(
                "La empresa no tiene cuenta de detracción del Banco de la Nación configurada"
            );
        }

        // <cac:PaymentMeans>
        Element paymentMeans = doc.createElementNS(NS_CAC, "cac:PaymentMeans");

        // <cbc:ID>Detraccion</cbc:ID>
        addCbcElement(doc, paymentMeans, "ID", "Detraccion");

        // <cbc:PaymentMeansCode>003</cbc:PaymentMeansCode>
        Element paymentMeansCode = addCbcElement(doc, paymentMeans, "PaymentMeansCode", "003");
        paymentMeansCode.setAttribute("listAgencyName", "PE:SUNAT");
        paymentMeansCode.setAttribute("listName", "Medio de pago");
        paymentMeansCode.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo59");

        // <cac:PayeeFinancialAccount>
        Element payeeAccount = doc.createElementNS(NS_CAC, "cac:PayeeFinancialAccount");

        // <cbc:ID>[cuenta BN]</cbc:ID>
        addCbcElement(doc, payeeAccount, "ID", empresa.getCuentaDetraccionBN());

        paymentMeans.appendChild(payeeAccount);
        root.appendChild(paymentMeans);
    }

    /**
     * Genera el bloque PaymentTerms para detracción con código, porcentaje y monto.
     */
    private void buildPaymentTermsDetraccion(Document doc, Element root,
                                              ComprobanteRequestDTO comprobante) {
        if (comprobante.getCodigoDetraccion() == null || comprobante.getCodigoDetraccion().isBlank()) {
            throw new IllegalArgumentException("Código de detracción es obligatorio");
        }
        if (comprobante.getPorcentajeDetraccion() == null) {
            throw new IllegalArgumentException("Porcentaje de detracción es obligatorio");
        }
        if (comprobante.getMontoDetraccion() == null) {
            throw new IllegalArgumentException("Monto de detracción es obligatorio");
        }

        // <cac:PaymentTerms>
        Element paymentTerms = doc.createElementNS(NS_CAC, "cac:PaymentTerms");

        // <cbc:ID>Detraccion</cbc:ID>
        addCbcElement(doc, paymentTerms, "ID", "Detraccion");

        // <cbc:PaymentMeansID>[código catálogo 54]</cbc:PaymentMeansID>
        Element paymentMeansID = addCbcElement(doc, paymentTerms, "PaymentMeansID",
                comprobante.getCodigoDetraccion());
        paymentMeansID.setAttribute("schemeName", "SUNAT:Codigo de detraccion");
        paymentMeansID.setAttribute("schemeAgencyName", "PE:SUNAT");
        paymentMeansID.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo54");

        // <cbc:PaymentPercent>[porcentaje]</cbc:PaymentPercent>
        addCbcElement(doc, paymentTerms, "PaymentPercent",
                comprobante.getPorcentajeDetraccion().setScale(2, RoundingMode.HALF_UP).toPlainString());

        // <cbc:Amount currencyID="PEN">[monto]</cbc:Amount>
        Element amount = addCbcElement(doc, paymentTerms, "Amount",
                comprobante.getMontoDetraccion().setScale(2, RoundingMode.HALF_UP).toPlainString());
        amount.setAttribute("currencyID", comprobante.getMoneda() != null 
                ? comprobante.getMoneda() : "PEN");

        root.appendChild(paymentTerms);
    }
}


