package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.GuiaRemisionRequestDTO;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;

/**
 * Genera XML UBL 2.1 para Guía de Remisión Electrónica (DespatchAdvice)
 */
@Service
public class GuiaRemisionXmlBuilderService {

    public Document buildXml(GuiaRemisionRequestDTO request) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        EmpresaDTO empresa = request.getEmpresa();
        LocalDate fechaEmision = request.getFechaEmision() != null ? request.getFechaEmision() : LocalDate.now();

        // Raíz: DespatchAdvice
        Element root = doc.createElementNS(NS_DESPATCH, "DespatchAdvice");
        root.setAttribute("xmlns", NS_DESPATCH);
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:ds", NS_DS);
        doc.appendChild(root);

        // UBLExtensions
        buildUblExtensions(doc, root);

        addCbcElement(doc, root, "UBLVersionID", UBL_VERSION);
        addCbcElement(doc, root, "CustomizationID", "2.0");
        addCbcElement(doc, root, "ID", request.getSerieCorrelativo());
        addCbcElement(doc, root, "IssueDate", fechaEmision.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // DespatchAdviceTypeCode
        addCbcElement(doc, root, "DespatchAdviceTypeCode", request.getTipoGuia());

        // Note (descripción motivo)
        addCbcElement(doc, root, "Note", request.getDescripcionMotivo());

        // Signature
        buildSignature(doc, root, empresa);

        // DespatchSupplierParty (remitente)
        Element despatchSupplier = doc.createElementNS(NS_CAC, "cac:DespatchSupplierParty");
        Element supplierParty = doc.createElementNS(NS_CAC, "cac:Party");
        Element supplierPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element supplierIdEl = addCbcElement(doc, supplierPartyId, "ID", empresa.getRuc());
        supplierIdEl.setAttribute("schemeID", "6");
        supplierParty.appendChild(supplierPartyId);

        Element supplierLegal = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, supplierLegal, "RegistrationName", empresa.getRazonSocial());
        supplierParty.appendChild(supplierLegal);
        despatchSupplier.appendChild(supplierParty);
        root.appendChild(despatchSupplier);

        // DeliveryCustomerParty (destinatario)
        Element deliveryCustomer = doc.createElementNS(NS_CAC, "cac:DeliveryCustomerParty");
        Element customerParty = doc.createElementNS(NS_CAC, "cac:Party");
        Element customerPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element customerIdEl = addCbcElement(doc, customerPartyId, "ID", request.getDestinatarioNumeroDocumento());
        customerIdEl.setAttribute("schemeID", request.getDestinatarioTipoDocumento());
        customerParty.appendChild(customerPartyId);

        Element customerLegal = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, customerLegal, "RegistrationName", request.getDestinatarioRazonSocial());
        customerParty.appendChild(customerLegal);
        deliveryCustomer.appendChild(customerParty);
        root.appendChild(deliveryCustomer);

        // Shipment
        Element shipment = doc.createElementNS(NS_CAC, "cac:Shipment");
        addCbcElement(doc, shipment, "ID", "1");

        // HandlingCode (motivo traslado)
        addCbcElement(doc, shipment, "HandlingCode", request.getMotivoTraslado());
        addCbcElement(doc, shipment, "Information", request.getDescripcionMotivo());

        // Peso bruto
        Element grossWeight = addCbcElement(doc, shipment, "GrossWeightMeasure",
                request.getPesoBrutoTotal().toPlainString());
        grossWeight.setAttribute("unitCode", request.getUnidadPeso());

        // ShipmentStage - modalidad transporte
        Element shipmentStage = doc.createElementNS(NS_CAC, "cac:ShipmentStage");
        addCbcElement(doc, shipmentStage, "TransportModeCode", request.getModalidadTransporte());

        // Fecha inicio traslado
        Element transitPeriod = doc.createElementNS(NS_CAC, "cac:TransitPeriod");
        addCbcElement(doc, transitPeriod, "StartDate",
                request.getFechaInicioTraslado().format(DateTimeFormatter.ISO_LOCAL_DATE));
        shipmentStage.appendChild(transitPeriod);

        // Transportista (si público)
        if ("01".equals(request.getModalidadTransporte()) && request.getTransportistaTipoDocumento() != null) {
            Element carrierParty = doc.createElementNS(NS_CAC, "cac:CarrierParty");
            Element carrierPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
            Element carrierIdEl = addCbcElement(doc, carrierPartyId, "ID", request.getTransportistaNumeroDocumento());
            carrierIdEl.setAttribute("schemeID", request.getTransportistaTipoDocumento());
            carrierParty.appendChild(carrierPartyId);

            Element carrierName = doc.createElementNS(NS_CAC, "cac:PartyName");
            addCbcElement(doc, carrierName, "Name", request.getTransportistaRazonSocial());
            carrierParty.appendChild(carrierName);
            shipmentStage.appendChild(carrierParty);
        }

        // Conductor (si privado)
        if ("02".equals(request.getModalidadTransporte()) && request.getConductorNumeroDocumento() != null) {
            Element driverPerson = doc.createElementNS(NS_CAC, "cac:DriverPerson");
            Element driverId = addCbcElement(doc, driverPerson, "ID", request.getConductorNumeroDocumento());
            driverId.setAttribute("schemeID", request.getConductorTipoDocumento());
            addCbcElement(doc, driverPerson, "FirstName", request.getConductorNombre());
            if (request.getConductorLicencia() != null) {
                addCbcElement(doc, driverPerson, "JobTitle", request.getConductorLicencia());
            }
            shipmentStage.appendChild(driverPerson);
        }

        shipment.appendChild(shipmentStage);

        // Delivery - direcciones
        Element delivery = doc.createElementNS(NS_CAC, "cac:Delivery");

        // Dirección de entrega (llegada)
        Element deliveryAddress = doc.createElementNS(NS_CAC, "cac:DeliveryAddress");
        addCbcElement(doc, deliveryAddress, "ID", request.getLlegadaUbigeo());
        Element addressLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
        addCbcElement(doc, addressLine, "Line", request.getLlegadaDireccion());
        deliveryAddress.appendChild(addressLine);
        delivery.appendChild(deliveryAddress);

        // Dirección de despacho (partida)
        Element despatchAddress = doc.createElementNS(NS_CAC, "cac:Despatch");
        Element despatchAddr = doc.createElementNS(NS_CAC, "cac:DespatchAddress");
        addCbcElement(doc, despatchAddr, "ID", request.getPartidaUbigeo());
        Element despatchAddrLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
        addCbcElement(doc, despatchAddrLine, "Line", request.getPartidaDireccion());
        despatchAddr.appendChild(despatchAddrLine);
        despatchAddress.appendChild(despatchAddr);
        delivery.appendChild(despatchAddress);

        shipment.appendChild(delivery);

        // Placa del vehículo (transporte privado)
        if (request.getNumeroPlaca() != null) {
            Element transportHandling = doc.createElementNS(NS_CAC, "cac:TransportHandlingUnit");
            Element transportEquipment = doc.createElementNS(NS_CAC, "cac:TransportEquipment");
            addCbcElement(doc, transportEquipment, "ID", request.getNumeroPlaca());
            transportHandling.appendChild(transportEquipment);
            shipment.appendChild(transportHandling);
        }

        root.appendChild(shipment);

        // Documento relacionado (opcional)
        if (request.getDocRelacionadoTipo() != null && request.getDocRelacionadoNumero() != null) {
            Element additionalDocRef = doc.createElementNS(NS_CAC, "cac:AdditionalDocumentReference");
            addCbcElement(doc, additionalDocRef, "ID", request.getDocRelacionadoNumero());
            addCbcElement(doc, additionalDocRef, "DocumentTypeCode", request.getDocRelacionadoTipo());
            root.appendChild(additionalDocRef);
        }

        // DespatchLine (items)
        for (GuiaRemisionRequestDTO.GuiaItemDTO item : request.getItems()) {
            Element line = doc.createElementNS(NS_CAC, "cac:DespatchLine");
            addCbcElement(doc, line, "ID", String.valueOf(item.getNumero()));

            Element deliveredQty = addCbcElement(doc, line, "DeliveredQuantity",
                    item.getCantidad().toPlainString());
            deliveredQty.setAttribute("unitCode", item.getUnidadMedida());

            Element orderLineRef = doc.createElementNS(NS_CAC, "cac:OrderLineReference");
            addCbcElement(doc, orderLineRef, "LineID", String.valueOf(item.getNumero()));
            line.appendChild(orderLineRef);

            Element itemEl = doc.createElementNS(NS_CAC, "cac:Item");
            addCbcElement(doc, itemEl, "Name", item.getDescripcion());

            if (item.getCodigoProducto() != null) {
                Element sellersId = doc.createElementNS(NS_CAC, "cac:SellersItemIdentification");
                addCbcElement(doc, sellersId, "ID", item.getCodigoProducto());
                itemEl.appendChild(sellersId);
            }
            line.appendChild(itemEl);

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

