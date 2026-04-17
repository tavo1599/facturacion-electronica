package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.RetencionRequestDTO;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;

@Service
public class RetencionXmlBuilderService {

    public Document buildXml(RetencionRequestDTO request) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        EmpresaDTO empresa = request.getEmpresa();
        LocalDate fecha = request.getFechaEmision() != null ? request.getFechaEmision() : LocalDate.now();
        String moneda = request.getMoneda();

        Element root = doc.createElementNS(NS_RETENTION, "Retention");
        root.setAttribute("xmlns", NS_RETENTION);
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:ds", NS_DS);
        root.setAttribute("xmlns:sac", NS_SAC);
        doc.appendChild(root);

        buildUblExtensions(doc, root);

        addCbcElement(doc, root, "UBLVersionID", UBL_VERSION);
        addCbcElement(doc, root, "CustomizationID", CUSTOMIZATION_ID);

        // Signature
        buildSignature(doc, root, empresa);

        addCbcElement(doc, root, "ID", request.getSerieCorrelativo());
        addCbcElement(doc, root, "IssueDate", fecha.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // Agente (emisor de la retención)
        Element agentParty = doc.createElementNS(NS_CAC, "cac:AgentParty");
        Element agentPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element agentId = addCbcElement(doc, agentPartyId, "ID", empresa.getRuc());
        agentId.setAttribute("schemeID", "6");
        agentParty.appendChild(agentPartyId);
        Element agentPartyName = doc.createElementNS(NS_CAC, "cac:PartyName");
        addCbcElement(doc, agentPartyName, "Name", empresa.getRazonSocial());
        agentParty.appendChild(agentPartyName);
        Element agentLegal = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, agentLegal, "RegistrationName", empresa.getRazonSocial());
        agentParty.appendChild(agentLegal);
        root.appendChild(agentParty);

        // Proveedor (a quien se retiene)
        Element receiverParty = doc.createElementNS(NS_CAC, "cac:ReceiverParty");
        Element receiverPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element receiverId = addCbcElement(doc, receiverPartyId, "ID", request.getProveedorNumeroDocumento());
        receiverId.setAttribute("schemeID", request.getProveedorTipoDocumento());
        receiverParty.appendChild(receiverPartyId);
        Element receiverPartyName = doc.createElementNS(NS_CAC, "cac:PartyName");
        addCbcElement(doc, receiverPartyName, "Name", request.getProveedorRazonSocial());
        receiverParty.appendChild(receiverPartyName);
        Element receiverLegal = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, receiverLegal, "RegistrationName", request.getProveedorRazonSocial());
        receiverParty.appendChild(receiverLegal);
        root.appendChild(receiverParty);

        // Régimen de retención
        Element sacRegimen = doc.createElementNS(NS_SAC, "sac:SUNATRetentionSystemCode");
        sacRegimen.setTextContent(request.getRegimenRetencion());
        root.appendChild(sacRegimen);

        // Tasa
        Element sacPercent = doc.createElementNS(NS_SAC, "sac:SUNATRetentionPercent");
        sacPercent.setTextContent(request.getTasaRetencion().toPlainString());
        root.appendChild(sacPercent);

        // Observaciones
        if (request.getObservaciones() != null) {
            addCbcElement(doc, root, "Note", request.getObservaciones());
        }

        // Total retenido
        Element totalRetenido = addCbcElement(doc, root, "TotalInvoiceAmount",
                request.getTotalRetenido().toPlainString());
        totalRetenido.setAttribute("currencyID", moneda);

        // Total pagado
        Element totalPagado = doc.createElementNS(NS_SAC, "sac:SUNATTotalPaid");
        totalPagado.setTextContent(request.getTotalPagado().toPlainString());
        totalPagado.setAttribute("currencyID", moneda);
        root.appendChild(totalPagado);

        // Items
        for (RetencionRequestDTO.RetencionItemDTO item : request.getItems()) {
            Element line = doc.createElementNS(NS_SAC, "sac:SUNATRetentionDocumentReference");

            // Documento relacionado
            Element docRef = addCbcElement(doc, line, "ID", item.getNumDocRelacionado());
            addCbcElement(doc, line, "IssueDate",
                    item.getFechaDocRelacionado().format(DateTimeFormatter.ISO_LOCAL_DATE));

            Element totalInvoice = addCbcElement(doc, line, "TotalInvoiceAmount",
                    item.getImporteDocRelacionado().toPlainString());
            totalInvoice.setAttribute("currencyID", item.getMonedaDocRelacionado());

            // Pago
            Element payment = doc.createElementNS(NS_CAC, "cac:Payment");
            addCbcElement(doc, payment, "ID", item.getNumeroPago());
            Element paidAmount = addCbcElement(doc, payment, "PaidAmount", item.getImportePagado().toPlainString());
            paidAmount.setAttribute("currencyID", item.getMonedaPago());
            addCbcElement(doc, payment, "PaidDate",
                    item.getFechaPago().format(DateTimeFormatter.ISO_LOCAL_DATE));
            line.appendChild(payment);

            // Monto retenido
            Element retAmount = doc.createElementNS(NS_SAC, "sac:SUNATRetentionAmount");
            retAmount.setTextContent(item.getMontoRetenido().toPlainString());
            retAmount.setAttribute("currencyID", item.getMonedaRetencion());
            line.appendChild(retAmount);

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

    private Element addCbcElement(Document doc, Element parent, String localName, String value) {
        Element element = doc.createElementNS(NS_CBC, "cbc:" + localName);
        if (value != null) element.setTextContent(value);
        parent.appendChild(element);
        return element;
    }
}

