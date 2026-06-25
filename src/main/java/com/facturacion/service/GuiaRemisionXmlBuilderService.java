package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.GuiaRemisionRequestDTO;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.facturacion.util.UblConstants.*;

/**
 * Genera XML UBL 2.1 para Guía de Remisión Electrónica (DespatchAdvice)
 * Estructura alineada al formato GRE 2.0 de SUNAT.
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

        // Raíz: DespatchAdvice con todos los namespaces requeridos por SUNAT GRE 2.0
        Element root = doc.createElementNS(NS_DESPATCH, "DespatchAdvice");
        root.setAttribute("xmlns", NS_DESPATCH);
        root.setAttribute("xmlns:ds", NS_DS);
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xmlns:qdt", "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2");
        root.setAttribute("xmlns:sac", "urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1");
        root.setAttribute("xmlns:ext", NS_EXT);
        root.setAttribute("xmlns:udt", "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2");
        root.setAttribute("xmlns:cac", NS_CAC);
        root.setAttribute("xmlns:cbc", NS_CBC);
        root.setAttribute("xmlns:ccts", "urn:un:unece:uncefact:documentation:2");
        doc.appendChild(root);

        // UBLExtensions (firma digital — se completa en SignatureService)
        buildUblExtensions(doc, root);

        addCbcElement(doc, root, "UBLVersionID", UBL_VERSION);
        addCbcElement(doc, root, "CustomizationID", "2.0");
        addCbcElement(doc, root, "ID", request.getSerieCorrelativo());
        addCbcElement(doc, root, "IssueDate", fechaEmision.format(DateTimeFormatter.ISO_LOCAL_DATE));
        addCbcElement(doc, root, "IssueTime", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        // DispatchAdviceTypeCode sin atributos extra (SUNAT GRE 2.0 no los requiere)
        addCbcElement(doc, root, "DespatchAdviceTypeCode", request.getTipoGuia());
        addCbcElement(doc, root, "Note", request.getDescripcionMotivo());

        // Signature
        buildSignature(doc, root, empresa);

        // DespatchSupplierParty (remitente)
        Element despatchSupplier = doc.createElementNS(NS_CAC, "cac:DespatchSupplierParty");

        // CustomerAssignedAccountID requerido por SUNAT GRE 2.0
        Element custAccId = addCbcElement(doc, despatchSupplier, "CustomerAssignedAccountID", empresa.getRuc());
        custAccId.setAttribute("schemeID", "6");

        Element supplierParty = doc.createElementNS(NS_CAC, "cac:Party");
        Element supplierPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
        Element supplierIdEl = addCbcElement(doc, supplierPartyId, "ID", empresa.getRuc());
        supplierIdEl.setAttribute("schemeID", "6");
        supplierIdEl.setAttribute("schemeName", "Documento de Identidad");
        supplierIdEl.setAttribute("schemeAgencyName", "PE:SUNAT");
        supplierIdEl.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
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
        customerIdEl.setAttribute("schemeName", "Documento de Identidad");
        customerIdEl.setAttribute("schemeAgencyName", "PE:SUNAT");
        customerIdEl.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
        customerParty.appendChild(customerPartyId);

        Element customerLegal = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
        addCbcElement(doc, customerLegal, "RegistrationName", request.getDestinatarioRazonSocial());
        customerParty.appendChild(customerLegal);
        deliveryCustomer.appendChild(customerParty);
        root.appendChild(deliveryCustomer);

        // Shipment
        Element shipment = doc.createElementNS(NS_CAC, "cac:Shipment");
        // ID fijo requerido por SUNAT GRE 2.0
        addCbcElement(doc, shipment, "ID", "SUNAT_Envio");

        Element handlingCode = addCbcElement(doc, shipment, "HandlingCode", request.getMotivoTraslado());
        handlingCode.setAttribute("listAgencyName", "PE:SUNAT");
        handlingCode.setAttribute("listName", "Motivo de traslado");
        handlingCode.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo20");

        Element grossWeight = addCbcElement(doc, shipment, "GrossWeightMeasure",
                request.getPesoBrutoTotal().toPlainString());
        grossWeight.setAttribute("unitCode", request.getUnidadPeso());

        // ShipmentStage
        Element shipmentStage = doc.createElementNS(NS_CAC, "cac:ShipmentStage");
        Element transportModeCode = addCbcElement(doc, shipmentStage, "TransportModeCode", request.getModalidadTransporte());
        transportModeCode.setAttribute("listAgencyName", "PE:SUNAT");
        transportModeCode.setAttribute("listName", "Modalidad de traslado");
        transportModeCode.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo18");

        Element transitPeriod = doc.createElementNS(NS_CAC, "cac:TransitPeriod");
        addCbcElement(doc, transitPeriod, "StartDate",
                request.getFechaInicioTraslado().format(DateTimeFormatter.ISO_LOCAL_DATE));
        shipmentStage.appendChild(transitPeriod);

        // Transportista (modalidad pública 01)
        if ("01".equals(request.getModalidadTransporte()) && request.getTransportistaTipoDocumento() != null) {
            Element carrierParty = doc.createElementNS(NS_CAC, "cac:CarrierParty");
            Element carrierPartyId = doc.createElementNS(NS_CAC, "cac:PartyIdentification");
            Element carrierIdEl = addCbcElement(doc, carrierPartyId, "ID", request.getTransportistaNumeroDocumento());
            carrierIdEl.setAttribute("schemeID", request.getTransportistaTipoDocumento());
            carrierParty.appendChild(carrierPartyId);

            Element carrierLegal = doc.createElementNS(NS_CAC, "cac:PartyLegalEntity");
            addCbcElement(doc, carrierLegal, "RegistrationName", request.getTransportistaRazonSocial());
            carrierParty.appendChild(carrierLegal);
            shipmentStage.appendChild(carrierParty);
        }

        // Conductor (modalidad privada 02)
        // Conductor (modalidad privada 02)
if ("02".equals(request.getModalidadTransporte()) && request.getConductorNumeroDocumento() != null) {
    Element driverPerson = doc.createElementNS(NS_CAC, "cac:DriverPerson");
    
    // DNI del conductor
    Element driverId = addCbcElement(doc, driverPerson, "ID", request.getConductorNumeroDocumento());
    driverId.setAttribute("schemeID", request.getConductorTipoDocumento());
    driverId.setAttribute("schemeName", "Documento de Identidad");
    driverId.setAttribute("schemeAgencyName", "PE:SUNAT");
    driverId.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
    
    // Nombres del conductor
    if (request.getConductorNombre() != null) {
        addCbcElement(doc, driverPerson, "FirstName", request.getConductorNombre());
    }
    
    // FamilyName: SUNAT lo requiere
    addCbcElement(doc, driverPerson, "FamilyName", request.getConductorNombre() != null ? request.getConductorNombre() : "Conductor");
    
    // JobTitle es REQUERIDO con valor "Principal" (no la licencia)
    addCbcElement(doc, driverPerson, "JobTitle", "Principal");
    
    // Licencia: va en cac:IdentityDocumentReference
    if (request.getConductorLicencia() != null) {
        Element idDocRef = doc.createElementNS(NS_CAC, "cac:IdentityDocumentReference");
        addCbcElement(doc, idDocRef, "ID", request.getConductorLicencia());
        driverPerson.appendChild(idDocRef);
    }
    
    shipmentStage.appendChild(driverPerson);
}

        shipment.appendChild(shipmentStage);

        // Delivery — direcciones con atributos de ubigeo requeridos por SUNAT
        Element delivery = doc.createElementNS(NS_CAC, "cac:Delivery");

        Element deliveryAddress = doc.createElementNS(NS_CAC, "cac:DeliveryAddress");
        Element deliveryUbigeoId = addCbcElement(doc, deliveryAddress, "ID", request.getLlegadaUbigeo());
        deliveryUbigeoId.setAttribute("schemeAgencyName", "PE:INEI");
        deliveryUbigeoId.setAttribute("schemeName", "Ubigeos");
        Element addressLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
        addCbcElement(doc, addressLine, "Line", request.getLlegadaDireccion());
        deliveryAddress.appendChild(addressLine);
        delivery.appendChild(deliveryAddress);

        Element despatchEl = doc.createElementNS(NS_CAC, "cac:Despatch");
        Element despatchAddr = doc.createElementNS(NS_CAC, "cac:DespatchAddress");
        Element partidaUbigeoId = addCbcElement(doc, despatchAddr, "ID", request.getPartidaUbigeo());
        partidaUbigeoId.setAttribute("schemeAgencyName", "PE:INEI");
        partidaUbigeoId.setAttribute("schemeName", "Ubigeos");
        Element despatchAddrLine = doc.createElementNS(NS_CAC, "cac:AddressLine");
        addCbcElement(doc, despatchAddrLine, "Line", request.getPartidaDireccion());
        despatchAddr.appendChild(despatchAddrLine);
        despatchEl.appendChild(despatchAddr);
        delivery.appendChild(despatchEl);

        shipment.appendChild(delivery);

        // Placa del vehículo (transporte privado)
        if (request.getNumeroPlaca() != null) {
            Element transportHandling = doc.createElementNS(NS_CAC, "cac:TransportHandlingUnit");
            Element transportEquipment = doc.createElementNS(NS_CAC, "cac:TransportEquipment");
            Element placaId = addCbcElement(doc, transportEquipment, "ID", request.getNumeroPlaca());
            placaId.setAttribute("schemeID", "1");
            transportHandling.appendChild(transportEquipment);
            shipment.appendChild(transportHandling);
        }

        root.appendChild(shipment);

        // DespatchLine (items)
        for (GuiaRemisionRequestDTO.GuiaItemDTO item : request.getItems()) {
            Element line = doc.createElementNS(NS_CAC, "cac:DespatchLine");
            addCbcElement(doc, line, "ID", String.valueOf(item.getNumero()));

            Element deliveredQty = addCbcElement(doc, line, "DeliveredQuantity",
                    item.getCantidad().toPlainString());
            deliveredQty.setAttribute("unitCode", item.getUnidadMedida());
            deliveredQty.setAttribute("unitCodeListID", "UN/ECE rec 20");
            deliveredQty.setAttribute("unitCodeListAgencyName", "United Nations Economic Commission for Europe");

            Element orderLineRef = doc.createElementNS(NS_CAC, "cac:OrderLineReference");
            addCbcElement(doc, orderLineRef, "LineID", String.valueOf(item.getNumero()));
            line.appendChild(orderLineRef);

            Element itemEl = doc.createElementNS(NS_CAC, "cac:Item");
            // SUNAT GRE usa Description (no Name)
            addCbcElement(doc, itemEl, "Description", item.getDescripcion());

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
        // ID y URI usan el RUC del emisor (formato SUNAT GRE 2.0)
        addCbcElement(doc, signature, "ID", empresa.getRuc());

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
        addCbcElement(doc, extRef, "URI", empresa.getRuc());
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
