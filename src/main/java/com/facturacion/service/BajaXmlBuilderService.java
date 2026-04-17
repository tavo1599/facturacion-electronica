package com.facturacion.service;

import com.facturacion.dto.BajaRequestDTO;
import com.facturacion.dto.EmpresaDTO;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;

/**
 * Genera XML para Comunicación de Baja (VoidedDocuments) según SUNAT
 */
@Service
public class BajaXmlBuilderService {

    public Document buildXml(BajaRequestDTO request) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        EmpresaDTO empresa = request.getEmpresa();
        LocalDate fechaGen = request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now();

        // Raíz: VoidedDocuments
        Element root = doc.createElementNS(NS_VOIDED, "VoidedDocuments");
        root.setAttribute("xmlns", NS_VOIDED);
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:sac", NS_SAC);
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:ds", NS_DS);
        doc.appendChild(root);

        // UBLExtensions (para firma)
        buildUblExtensions(doc, root);

        // UBLVersionID - VoidedDocuments usa 2.0
        addCbcElement(doc, root, "UBLVersionID", "2.0");

        // CustomizationID - VoidedDocuments usa 1.0
        addCbcElement(doc, root, "CustomizationID", "1.0");

        // ID: RA-{fecha}-{correlativo}
        addCbcElement(doc, root, "ID", request.getIdentificador());

        // ReferenceDate: fecha de emisión de los docs dados de baja
        addCbcElement(doc, root, "ReferenceDate",
                request.getFechaBajaDocs().format(DateTimeFormatter.ISO_LOCAL_DATE));

        // IssueDate: fecha de generación
        addCbcElement(doc, root, "IssueDate",
                fechaGen.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // Signature
        buildSignature(doc, root, empresa);

        // AccountingSupplierParty
        buildSupplier(doc, root, empresa);

        // VoidedDocumentsLine (cada documento dado de baja)
        int lineNumber = 1;
        for (BajaRequestDTO.BajaItemDTO item : request.getItems()) {
            Element line = doc.createElementNS(NS_SAC, "sac:VoidedDocumentsLine");

            addCbcElement(doc, line, "LineID", String.valueOf(lineNumber++));

            // Tipo de documento (namespace cbc según schema SUNAT)
            addCbcElement(doc, line, "DocumentTypeCode", item.getTipoDocumento());

            // Serie-Correlativo del documento
            Element docSerialId = doc.createElementNS(NS_SAC, "sac:DocumentSerialID");
            docSerialId.setTextContent(item.getSerie());
            line.appendChild(docSerialId);

            Element docNumberId = doc.createElementNS(NS_SAC, "sac:DocumentNumberID");
            docNumberId.setTextContent(String.valueOf(item.getCorrelativo()));
            line.appendChild(docNumberId);

            // Motivo (namespace sac según schema SUNAT)
            Element voidReasonDesc = doc.createElementNS(NS_SAC, "sac:VoidReasonDescription");
            voidReasonDesc.setTextContent(item.getMotivo());
            line.appendChild(voidReasonDesc);

            root.appendChild(line);
        }

        return doc;
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

        Element customerAssignedId = addCbcElement(doc, supplier, "CustomerAssignedAccountID", empresa.getRuc());

        Element additionalId = addCbcElement(doc, supplier, "AdditionalAccountID", "6");

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

