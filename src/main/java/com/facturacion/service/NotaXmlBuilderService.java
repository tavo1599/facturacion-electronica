package com.facturacion.service;

import com.facturacion.dto.ComprobanteLineaDTO;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.NotaRequestDTO;
import com.facturacion.util.MontoEnLetras;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;

/**
 *
 * Diferencias principales con Invoice:
 * - Elemento raíz: CreditNote o DebitNote (no Invoice)
 * - Namespace raíz diferente
 * - Incluye BillingReference (referencia al comprobante que modifica)
 * - Incluye DiscrepancyResponse (motivo de la nota)
 * - Las líneas se llaman CreditNoteLine / DebitNoteLine (no InvoiceLine)
 * - La cantidad se llama CreditedQuantity / DebitedQuantity (no InvoicedQuantity)
 */
@Service
public class NotaXmlBuilderService {


    /**
     * Genera el XML UBL 2.1 para una Nota de Crédito o Débito
     */
    public Document buildXml(NotaRequestDTO nota) throws Exception {
        boolean esCreditNote = "07".equals(nota.getTipoNota());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        // === NAMESPACE RAÍZ ===
        String rootNs = esCreditNote ? NS_CREDIT_NOTE : NS_DEBIT_NOTE;
        String rootTag = esCreditNote ? "CreditNote" : "DebitNote";

        Element root = doc.createElementNS(rootNs, rootTag);
        root.setAttribute("xmlns", rootNs);
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:ds", NS_DS);
        doc.appendChild(root);

        // 1. UBLExtensions (para firma digital)
        buildUblExtensions(doc, root);

        // 2. UBLVersionID
        addCbcElement(doc, root, "UBLVersionID", UBL_VERSION);

        // 3. CustomizationID
        addCbcElement(doc, root, "CustomizationID", CUSTOMIZATION_ID);

        // 4. ID - Serie y correlativo
        addCbcElement(doc, root, "ID", nota.getSerieCorrelativo());

        // 5. IssueDate
        LocalDate fecha = nota.getFechaEmision() != null ? nota.getFechaEmision() : LocalDate.now();
        addCbcElement(doc, root, "IssueDate", fecha.format(DateTimeFormatter.ISO_DATE));

        // 6. IssueTime
        String hora = nota.getHoraEmision() != null
                ? nota.getHoraEmision()
                : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        addCbcElement(doc, root, "IssueTime", hora);

        // 7. Note - Leyenda (monto en letras)
        Element notaElement = addCbcElement(doc, root, "Note",
                MontoEnLetras.convertir(nota.getImporteTotal(), nota.getMoneda()));
        notaElement.setAttribute("languageLocaleID", "1000");

        // 8. DocumentCurrencyCode
        Element currencyCode = addCbcElement(doc, root, "DocumentCurrencyCode", nota.getMoneda());
        currencyCode.setAttribute("listID", "ISO 4217 Alpha");
        currencyCode.setAttribute("listName", "Currency");
        currencyCode.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");

        // 9. DiscrepancyResponse - MOTIVO de la nota
        buildDiscrepancyResponse(doc, root, nota, esCreditNote);

        // 10. BillingReference - COMPROBANTE QUE MODIFICA
        buildBillingReference(doc, root, nota);

        // 11. Signature (referencia)
        EmpresaDTO empresa = nota.getEmpresa();
        buildSignatureReference(doc, root, empresa);

        // 12. AccountingSupplierParty (emisor)
        buildSupplierParty(doc, root, empresa);

        // 13. AccountingCustomerParty (cliente)
        buildCustomerParty(doc, root, nota);

        // 14. TaxTotal
        buildTaxTotal(doc, root, nota);

        // 15. LegalMonetaryTotal
        buildLegalMonetaryTotal(doc, root, nota);

        // 16. Líneas de detalle
        String lineTag = esCreditNote ? "CreditNoteLine" : "DebitNoteLine";
        String qtyTag = esCreditNote ? "CreditedQuantity" : "DebitedQuantity";

        for (ComprobanteLineaDTO linea : nota.getItems()) {
            buildNoteLine(doc, root, linea, nota.getMoneda(), lineTag, qtyTag);
        }

        return doc;
    }

    // ==================== SECCIONES ESPECÍFICAS DE NOTAS ====================

    /**
     * DiscrepancyResponse - Motivo de la nota
     * Catálogo 09 (NC) o Catálogo 10 (ND)
     */
    private void buildDiscrepancyResponse(Document doc, Element root,
                                           NotaRequestDTO nota, boolean esCreditNote) {
        Element discrepancy = doc.createElementNS(NS_CAC, "cac:DiscrepancyResponse");

        // ReferenceID = Serie-Correlativo del comprobante afectado
        addCbcElement(doc, discrepancy, "ReferenceID", nota.getComprobanteAfectado());

        // ResponseCode = Código del motivo
        Element responseCode = addCbcElement(doc, discrepancy, "ResponseCode", nota.getCodigoMotivo());
        responseCode.setAttribute("listAgencyName", AGENCY_NAME);
        responseCode.setAttribute("listName",
                esCreditNote ? "Tipo de nota de credito" : "Tipo de nota de debito");
        responseCode.setAttribute("listURI", SCHEME_URI);

        // Description = Descripción del motivo
        addCbcElement(doc, discrepancy, "Description", nota.getDescripcionMotivo());

        root.appendChild(discrepancy);
    }

    /**
     * BillingReference - Referencia al comprobante que se modifica
     */
    private void buildBillingReference(Document doc, Element root, NotaRequestDTO nota) {
        Element billingRef = doc.createElementNS(NS_CAC, "cac:BillingReference");
        Element invoiceDocRef = doc.createElementNS(NS_CAC, "cac:InvoiceDocumentReference");

        // ID del comprobante afectado
        addCbcElement(doc, invoiceDocRef, "ID", nota.getComprobanteAfectado());

        // Tipo del comprobante afectado
        Element docTypeCode = addCbcElement(doc, invoiceDocRef, "DocumentTypeCode",
                nota.getTipoComprobanteAfectado());
        docTypeCode.setAttribute("listAgencyName", AGENCY_NAME);
        docTypeCode.setAttribute("listName", "Tipo de Documento");
        docTypeCode.setAttribute("listURI", SCHEME_URI);

        billingRef.appendChild(invoiceDocRef);
        root.appendChild(billingRef);
    }

    // ==================== SECCIONES COMUNES ====================

    private void buildUblExtensions(Document doc, Element root) {
        Element extensions = doc.createElementNS(NS_EXT, "ext:UBLExtensions");
        Element extension = doc.createElementNS(NS_EXT, "ext:UBLExtension");
        Element content = doc.createElementNS(NS_EXT, "ext:ExtensionContent");
        extension.appendChild(content);
        extensions.appendChild(extension);
        root.appendChild(extensions);
    }

    private void buildSignatureReference(Document doc, Element root, EmpresaDTO empresa) {
        Element signature = doc.createElementNS(NS_CAC, "cac:Signature");
        addCbcElement(doc, signature, "ID", "IDSignSunat");

        Element signatoryParty = doc.createElementNS(NS_CAC, "cac:SignatoryParty");
        Element partyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        addCbcElement(doc, partyId, "ID", empresa.getRuc());
        signatoryParty.appendChild(partyId);

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

        Element address = doc.createElementNS(NS_CAC, "cac:RegistrationAddress");
        addCbcElement(doc, address, "ID", empresa.getUbigeo());
        addCbcElement(doc, address, "AddressTypeCode", "0000");
        addCbcElement(doc, address, "CitySubdivisionName", "-");
        addCbcElement(doc, address, "CityName", empresa.getProvincia());
        addCbcElement(doc, address, "CountrySubentity", empresa.getDepartamento());
        addCbcElement(doc, address, "District", empresa.getDistrito());

        Element addressLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
        addCbcElement(doc, addressLine, "Line", empresa.getDireccion());
        address.appendChild(addressLine);

        Element country = doc.createElementNS(NS_CAC, "cac:Country");
        addCbcElement(doc, country, "IdentificationCode", empresa.getCodigoPais());
        address.appendChild(country);

        legalEntity.appendChild(address);
        party.appendChild(legalEntity);
        supplier.appendChild(party);
        root.appendChild(supplier);
    }

    private void buildCustomerParty(Document doc, Element root, NotaRequestDTO nota) {
        Element customer = doc.createElementNS(NS_CAC, "cac:AccountingCustomerParty");
        Element party = doc.createElementNS(NS_CAC, "cac:Party");

        // PartyIdentification
        Element partyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element id = addCbcElement(doc, partyId, "ID", nota.getClienteNumeroDocumento());
        id.setAttribute("schemeID", nota.getClienteTipoDocumento());
        party.appendChild(partyId);

        Element legalEntity = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, legalEntity, "RegistrationName", nota.getClienteRazonSocial());

        if (nota.getClienteDireccion() != null && !nota.getClienteDireccion().isEmpty()) {
            Element address = doc.createElementNS(NS_CAC, "cac:RegistrationAddress");
            Element addressLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
            addCbcElement(doc, addressLine, "Line", nota.getClienteDireccion());
            address.appendChild(addressLine);
            legalEntity.appendChild(address);
        }

        party.appendChild(legalEntity);
        customer.appendChild(party);
        root.appendChild(customer);
    }

    private void buildTaxTotal(Document doc, Element root, NotaRequestDTO nota) {
        Element taxTotal = doc.createElementNS(NS_CAC, "cac:TaxTotal");

        Element taxAmount = addCbcElement(doc, taxTotal, "TaxAmount",
                nota.getTotalIgv().toPlainString());
        taxAmount.setAttribute("currencyID", nota.getMoneda());

        // Gravado
        if (nota.getTotalGravado().compareTo(BigDecimal.ZERO) > 0) {
            buildTaxSubtotal(doc, taxTotal, nota.getMoneda(),
                    nota.getTotalGravado(), nota.getTotalIgv(),
                    TRIBUTO_IGV_ID, TRIBUTO_IGV_NOMBRE, TRIBUTO_IGV_CODIGO);
        }

        // Exonerado
        if (nota.getTotalExonerado().compareTo(BigDecimal.ZERO) > 0) {
            buildTaxSubtotal(doc, taxTotal, nota.getMoneda(),
                    nota.getTotalExonerado(), BigDecimal.ZERO,
                    TRIBUTO_EXONERADO_ID, TRIBUTO_EXONERADO_NOMBRE, TRIBUTO_EXONERADO_CODIGO);
        }

        // Inafecto
        if (nota.getTotalInafecto().compareTo(BigDecimal.ZERO) > 0) {
            buildTaxSubtotal(doc, taxTotal, nota.getMoneda(),
                    nota.getTotalInafecto(), BigDecimal.ZERO,
                    TRIBUTO_INAFECTO_ID, TRIBUTO_INAFECTO_NOMBRE, TRIBUTO_INAFECTO_CODIGO);
        }

        root.appendChild(taxTotal);
    }

    private void buildTaxSubtotal(Document doc, Element parent, String moneda,
                                   BigDecimal base, BigDecimal monto,
                                   String tribId, String tribNombre, String tribCodigo) {
        Element taxSubtotal = doc.createElementNS(NS_CAC, "cac:TaxSubtotal");

        Element taxableAmount = addCbcElement(doc, taxSubtotal, "TaxableAmount", base.toPlainString());
        taxableAmount.setAttribute("currencyID", moneda);

        Element taxAmt = addCbcElement(doc, taxSubtotal, "TaxAmount", monto.toPlainString());
        taxAmt.setAttribute("currencyID", moneda);

        Element taxCategory = doc.createElementNS(NS_CAC, "cac:TaxCategory");

        Element taxScheme = doc.createElementNS(NS_CAC, "cac:TaxScheme");
        addCbcElement(doc, taxScheme, "ID", tribId);
        addCbcElement(doc, taxScheme, "Name", tribNombre);
        addCbcElement(doc, taxScheme, "TaxTypeCode", tribCodigo);

        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);
        parent.appendChild(taxSubtotal);
    }

    private void buildLegalMonetaryTotal(Document doc, Element root, NotaRequestDTO nota) {
        // Para Notas se usa RequestedMonetaryTotal (no LegalMonetaryTotal)
        Element monetaryTotal = doc.createElementNS(NS_CAC, "cac:RequestedMonetaryTotal");
        String moneda = nota.getMoneda();

        Element lineExtension = addCbcElement(doc, monetaryTotal, "LineExtensionAmount",
                nota.getTotalValorVenta().toPlainString());
        lineExtension.setAttribute("currencyID", moneda);

        Element taxInclusive = addCbcElement(doc, monetaryTotal, "TaxInclusiveAmount",
                nota.getImporteTotal().toPlainString());
        taxInclusive.setAttribute("currencyID", moneda);

        Element payable = addCbcElement(doc, monetaryTotal, "PayableAmount",
                nota.getImporteTotal().toPlainString());
        payable.setAttribute("currencyID", moneda);

        root.appendChild(monetaryTotal);
    }

    /**
     * Línea de detalle para Nota de Crédito / Débito
     * Usa CreditNoteLine/DebitNoteLine y CreditedQuantity/DebitedQuantity
     */
    private void buildNoteLine(Document doc, Element root, ComprobanteLineaDTO linea,
                                String moneda, String lineTag, String qtyTag) {

        Element noteLine = doc.createElementNS(NS_CAC, "cac:" + lineTag);

        // ID
        addCbcElement(doc, noteLine, "ID", String.valueOf(linea.getNumero()));

        // CreditedQuantity / DebitedQuantity
        Element quantity = addCbcElement(doc, noteLine, qtyTag,
                linea.getCantidad().toPlainString());
        quantity.setAttribute("unitCode", linea.getUnidadMedida());
        quantity.setAttribute("unitCodeListID", "UN/ECE rec 20");
        quantity.setAttribute("unitCodeListAgencyName", "United Nations Economic Commission for Europe");

        // LineExtensionAmount
        Element lineAmount = addCbcElement(doc, noteLine, "LineExtensionAmount",
                linea.getValorVenta().toPlainString());
        lineAmount.setAttribute("currencyID", moneda);

        // PricingReference
        Element pricingRef = doc.createElementNS(NS_CAC, "cac:PricingReference");
        Element altCondPrice = doc.createElementNS(NS_CAC, "cac:AlternativeConditionPrice");
        Element priceAmount = addCbcElement(doc, altCondPrice, "PriceAmount",
                linea.getPrecioUnitario().toPlainString());
        priceAmount.setAttribute("currencyID", moneda);

        Element priceTypeCode = addCbcElement(doc, altCondPrice, "PriceTypeCode", linea.getTipoPrecio());
        priceTypeCode.setAttribute("listName", "Tipo de Precio");
        priceTypeCode.setAttribute("listAgencyName", AGENCY_NAME);
        priceTypeCode.setAttribute("listURI", SCHEME_URI);

        pricingRef.appendChild(altCondPrice);
        noteLine.appendChild(pricingRef);

        // TaxTotal de la línea
        buildLineTaxTotal(doc, noteLine, linea, moneda);

        // Item
        Element item = doc.createElementNS(NS_CAC, "cac:Item");
        addCbcElement(doc, item, "Description", linea.getDescripcion());

        Element sellersId = doc.createElementNS(NS_CAC, "cac:SellersItemIdentification");
        addCbcElement(doc, sellersId, "ID", linea.getCodigoProducto());
        item.appendChild(sellersId);
        noteLine.appendChild(item);

        // Price
        Element price = doc.createElementNS(NS_CAC, "cac:Price");
        Element pAmount = addCbcElement(doc, price, "PriceAmount",
                linea.getValorUnitario().toPlainString());
        pAmount.setAttribute("currencyID", moneda);
        noteLine.appendChild(price);

        root.appendChild(noteLine);
    }

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
        String tribId, tribNombre, tribCodigo;
        switch (linea.getTipoAfectacionIgv()) {
            case "20": case "21":
                tribId = TRIBUTO_EXONERADO_ID; tribNombre = TRIBUTO_EXONERADO_NOMBRE;
                tribCodigo = TRIBUTO_EXONERADO_CODIGO; break;
            case "30": case "31": case "32": case "33": case "34": case "35": case "36":
                tribId = TRIBUTO_INAFECTO_ID; tribNombre = TRIBUTO_INAFECTO_NOMBRE;
                tribCodigo = TRIBUTO_INAFECTO_CODIGO; break;
            default:
                tribId = TRIBUTO_IGV_ID; tribNombre = TRIBUTO_IGV_NOMBRE;
                tribCodigo = TRIBUTO_IGV_CODIGO; break;
        }

        addCbcElement(doc, taxScheme, "ID", tribId);
        addCbcElement(doc, taxScheme, "Name", tribNombre);
        addCbcElement(doc, taxScheme, "TaxTypeCode", tribCodigo);

        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);
        taxTotal.appendChild(taxSubtotal);
        parent.appendChild(taxTotal);
    }

    // ==================== UTILIDADES ====================

    private Element addCbcElement(Document doc, Element parent, String localName, String value) {
        Element element = doc.createElementNS(NS_CBC, "cbc:" + localName);
        if (value != null) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
        return element;
    }
}
