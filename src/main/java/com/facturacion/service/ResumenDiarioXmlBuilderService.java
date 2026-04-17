package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.ResumenDiarioRequestDTO;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;

/**
 * Genera XML para Resumen Diario de Boletas (SummaryDocuments) según SUNAT
 */
@Service
public class ResumenDiarioXmlBuilderService {

    public Document buildXml(ResumenDiarioRequestDTO request) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        EmpresaDTO empresa = request.getEmpresa();
        LocalDate fechaGen = request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now();
        String moneda = request.getMoneda() != null ? request.getMoneda() : "PEN";

        // Raíz: SummaryDocuments
        Element root = doc.createElementNS(NS_SUMMARY, "SummaryDocuments");
        root.setAttribute("xmlns", NS_SUMMARY);
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:sac", NS_SAC);
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:ds", NS_DS);
        doc.appendChild(root);

        // UBLExtensions (para firma)
        buildUblExtensions(doc, root);

        addCbcElement(doc, root, "UBLVersionID", "2.0");
        addCbcElement(doc, root, "CustomizationID", "1.1");

        // ID: RC-{fecha}-{correlativo}
        addCbcElement(doc, root, "ID", request.getIdentificador());

        // ReferenceDate: fecha de las boletas
        addCbcElement(doc, root, "ReferenceDate",
                request.getFechaResumen().format(DateTimeFormatter.ISO_LOCAL_DATE));

        // IssueDate
        addCbcElement(doc, root, "IssueDate",
                fechaGen.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // Signature
        buildSignature(doc, root, empresa);

        // AccountingSupplierParty
        buildSupplier(doc, root, empresa);

        // SummaryDocumentsLine
        for (ResumenDiarioRequestDTO.ResumenItemDTO item : request.getItems()) {
            Element line = doc.createElementNS(NS_SAC, "sac:SummaryDocumentsLine");

            // 1. LineID
            addCbcElement(doc, line, "LineID", String.valueOf(item.getNumero()));

            // 2. DocumentTypeCode
            addCbcElement(doc, line, "DocumentTypeCode", item.getTipoDocumento());

            // 3. ID (serie-correlativo del documento)
            addCbcElement(doc, line, "ID", item.getSerieCorrelativo());

            // 4. AccountingCustomerParty
            Element customer = doc.createElementNS(NS_CAC, "cac:AccountingCustomerParty");
            addCbcElement(doc, customer, "CustomerAssignedAccountID", item.getClienteNumeroDocumento());
            addCbcElement(doc, customer, "AdditionalAccountID", item.getClienteTipoDocumento());
            line.appendChild(customer);

            // 5. Status (tipo operación: 1=adicionar, 2=modificar, 3=anular)
            Element status = doc.createElementNS(NS_CAC, "cac:Status");
            addCbcElement(doc, status, "ConditionCode", item.getTipoOperacion());
            line.appendChild(status);

            // 6. TotalAmount
            Element totalAmount = doc.createElementNS(NS_SAC, "sac:TotalAmount");
            totalAmount.setTextContent(item.getImporteTotal().toPlainString());
            totalAmount.setAttribute("currencyID", moneda);
            line.appendChild(totalAmount);

            // 7. BillingPayment (totales por tipo)
            addBillingPayment(doc, line, "01", item.getTotalGravado(), moneda);
            addBillingPayment(doc, line, "02", item.getTotalExonerado(), moneda);
            addBillingPayment(doc, line, "03", item.getTotalInafecto(), moneda);

            // 8. TaxTotal - IGV
            if (item.getTotalIgv().compareTo(BigDecimal.ZERO) != 0) {
                Element taxTotal = doc.createElementNS(NS_CAC, "cac:TaxTotal");
                Element taxAmount = addCbcElement(doc, taxTotal, "TaxAmount", item.getTotalIgv().toPlainString());
                taxAmount.setAttribute("currencyID", moneda);

                Element taxSubtotal = doc.createElementNS(NS_CAC, "cac:TaxSubtotal");
                Element subTaxAmount = addCbcElement(doc, taxSubtotal, "TaxAmount", item.getTotalIgv().toPlainString());
                subTaxAmount.setAttribute("currencyID", moneda);

                Element taxCategory = doc.createElementNS(NS_CAC, "cac:TaxCategory");
                addCbcElement(doc, taxCategory, "Percent", "18.00");
                Element taxScheme = doc.createElementNS(NS_CAC, "cac:TaxScheme");
                addCbcElement(doc, taxScheme, "ID", TRIBUTO_IGV_ID);
                addCbcElement(doc, taxScheme, "Name", TRIBUTO_IGV_NOMBRE);
                addCbcElement(doc, taxScheme, "TaxTypeCode", TRIBUTO_IGV_CODIGO);
                taxCategory.appendChild(taxScheme);
                taxSubtotal.appendChild(taxCategory);
                taxTotal.appendChild(taxSubtotal);
                line.appendChild(taxTotal);
            }

            root.appendChild(line);
        }

        return doc;
    }

    private void addBillingPayment(Document doc, Element parent, String code, BigDecimal amount, String moneda) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
            Element payment = doc.createElementNS(NS_SAC, "sac:BillingPayment");
            Element paidAmount = addCbcElement(doc, payment, "PaidAmount", amount.toPlainString());
            paidAmount.setAttribute("currencyID", moneda);
            Element instructionId = addCbcElement(doc, payment, "InstructionID", code);
            parent.appendChild(payment);
        }
    }

    private void buildUblExtensions(Document doc, Element root) {
        Element extensions = doc.createElementNS(NS_EXT, "ext:UBLExtensions");
        Element extension = doc.createElementNS(NS_EXT, "ext:UBLExtension");
        Element content = doc.createElementNS(NS_EXT, "ext:ExtensionContent");
        extension.appendChild(content);
        extensions.appendChild(extension);
        root.appendChild(extensions);
    }

    private void buildSignature(Document doc, Element root, EmpresaDTO empresa) {
        Element signature = doc.createElementNS(NS_CAC, "cac:Signature");
        addCbcElement(doc, signature, "ID", "IDSignSP");

        Element signatoryParty = doc.createElementNS(NS_CAC, "cac:SignatoryParty");
        Element partyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        addCbcElement(doc, partyId, "ID", empresa.getRuc());
        signatoryParty.appendChild(partyId);

        Element partyName = doc.createElementNS(NS_CAC, "cac:PartyName");
        addCbcElement(doc, partyName, "Name", empresa.getRazonSocial());
        signatoryParty.appendChild(partyName);
        signature.appendChild(signatoryParty);

        Element attachment = doc.createElementNS(NS_CAC, "cac:DigitalSignatureAttachment");
        Element extRef = doc.createElementNS(NS_CAC, "cac:ExternalReference");
        addCbcElement(doc, extRef, "URI", "#IDSignSP");
        attachment.appendChild(extRef);
        signature.appendChild(attachment);

        root.appendChild(signature);
    }

    private void buildSupplier(Document doc, Element root, EmpresaDTO empresa) {
        Element supplier = doc.createElementNS(NS_CAC, "cac:AccountingSupplierParty");
        addCbcElement(doc, supplier, "CustomerAssignedAccountID", empresa.getRuc());
        addCbcElement(doc, supplier, "AdditionalAccountID", "6");

        Element party = doc.createElementNS(NS_CAC, "cac:Party");
        Element partyLegalEntity = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, partyLegalEntity, "RegistrationName", empresa.getRazonSocial());
        party.appendChild(partyLegalEntity);
        supplier.appendChild(party);

        root.appendChild(supplier);
    }

    private Element addCbcElement(Document doc, Element parent, String localName, String value) {
        Element element = doc.createElementNS(NS_CBC, "cbc:" + localName);
        if (value != null) element.setTextContent(value);
        parent.appendChild(element);
        return element;
    }
}

