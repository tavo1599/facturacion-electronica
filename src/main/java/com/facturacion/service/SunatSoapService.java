package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.EmpresaDTO;
import jakarta.xml.soap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cliente SOAP para comunicarse con los web services de SUNAT
 * Soporta sendBill (envío individual) y sendSummary (resúmenes)
 */
@Service
public class SunatSoapService {

    private static final Logger log = LoggerFactory.getLogger(SunatSoapService.class);

    private static final String SOAP_ACTION_SEND_BILL = 
        "urn:service/sunat/ol/cpe/billService/sendBill";
    private static final String SOAP_ACTION_SEND_SUMMARY = 
        "urn:service/sunat/ol/cpe/billService/sendSummary";
    private static final String SOAP_ACTION_GET_STATUS = 
        "urn:service/sunat/ol/cpe/billService/getStatus";
    private static final String SERVICE_NS = 
        "http://service.sunat.gob.pe";

    private final SunatConfig config;
    private final XmlUtilService xmlUtilService;

    public SunatSoapService(SunatConfig config, XmlUtilService xmlUtilService) {
        this.config = config;
        this.xmlUtilService = xmlUtilService;
    }

    /**
     * Envía un comprobante individual a SUNAT (sendBill)
     * @param zipFileName nombre del archivo ZIP (ej: 20123456789-01-F001-00000001.zip)
     * @param zipContent contenido del ZIP en bytes
     * @return respuesta de SUNAT con el CDR
     */
    public SunatResponse sendBill(String zipFileName, byte[] zipContent, EmpresaDTO empresa) throws Exception {
        log.info("Enviando comprobante a SUNAT: {}", zipFileName);
        String endpoint = config.getEndpoint(empresa.getAmbiente());
        log.info("Endpoint: {}", endpoint);

        String zipBase64 = Base64.getEncoder().encodeToString(zipContent);

        String soapRequest = buildSoapRequest("sendBill", zipFileName, zipBase64, empresa);

        String responseXml = sendHttpRequest(soapRequest, SOAP_ACTION_SEND_BILL, endpoint);

        // Parsear respuesta
        return parseSunatResponse(responseXml);
    }

    /**
     * Envía un comprobante a SUNAT usando un endpoint específico (para Retenciones/Percepciones)
     */
    public SunatResponse sendBillToEndpoint(String zipFileName, byte[] zipContent, EmpresaDTO empresa, String endpoint) throws Exception {
        log.info("Enviando comprobante a SUNAT (CPE-GEM): {} → {}", zipFileName, endpoint);

        String zipBase64 = Base64.getEncoder().encodeToString(zipContent);
        String soapRequest = buildSoapRequest("sendBill", zipFileName, zipBase64, empresa);
        String responseXml = sendHttpRequest(soapRequest, SOAP_ACTION_SEND_BILL, endpoint);

        return parseSunatResponse(responseXml);
    }

    /**
     * Envía un resumen o comunicación de baja a SUNAT (sendSummary)
     * Retorna un ticket que luego se consulta con getStatus
     */
    public SunatTicketResponse sendSummary(String zipFileName, byte[] zipContent, EmpresaDTO empresa) throws Exception {
        log.info("Enviando resumen/baja a SUNAT: {}", zipFileName);
        String endpoint = config.getEndpoint(empresa.getAmbiente());

        String zipBase64 = Base64.getEncoder().encodeToString(zipContent);
        String soapRequest = buildSoapRequest("sendSummary", zipFileName, zipBase64, empresa);
        String responseXml = sendHttpRequest(soapRequest, SOAP_ACTION_SEND_SUMMARY, endpoint);

        return parseTicketResponse(responseXml);
    }

    /**
     * Consulta el estado de un ticket de resumen/baja (getStatus)
     */
    public SunatResponse getStatus(String ticket, EmpresaDTO empresa) throws Exception {
        log.info("Consultando estado de ticket: {}", ticket);
        String endpoint = config.getEndpoint(empresa.getAmbiente());

        String soapRequest = buildGetStatusRequest(ticket, empresa);
        String responseXml = sendHttpRequest(soapRequest, SOAP_ACTION_GET_STATUS, endpoint);

        return parseSunatResponse(responseXml);
    }

    /**
     * Construye el request SOAP para getStatus
     */
    private String buildGetStatusRequest(String ticket, EmpresaDTO empresa) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<soapenv:Envelope ");
        sb.append("xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        sb.append("xmlns:ser=\"http://service.sunat.gob.pe\" ");
        sb.append("xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
        sb.append("<soapenv:Header>");
        sb.append("<wsse:Security>");
        sb.append("<wsse:UsernameToken>");
        sb.append("<wsse:Username>").append(empresa.getSolUsername()).append("</wsse:Username>");
        sb.append("<wsse:Password>").append(empresa.getSolClave()).append("</wsse:Password>");
        sb.append("</wsse:UsernameToken>");
        sb.append("</wsse:Security>");
        sb.append("</soapenv:Header>");
        sb.append("<soapenv:Body>");
        sb.append("<ser:getStatus>");
        sb.append("<ticket>").append(ticket).append("</ticket>");
        sb.append("</ser:getStatus>");
        sb.append("</soapenv:Body>");
        sb.append("</soapenv:Envelope>");
        return sb.toString();
    }

    /**
     * Parsea la respuesta de sendSummary para extraer el ticket
     */
    private SunatTicketResponse parseTicketResponse(String responseXml) throws Exception {
        SunatTicketResponse result = new SunatTicketResponse();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(responseXml.getBytes(StandardCharsets.UTF_8)));

        // Buscar ticket
        NodeList ticketNodes = doc.getElementsByTagName("ticket");
        if (ticketNodes.getLength() > 0) {
            result.setTicket(ticketNodes.item(0).getTextContent().trim());
            result.setSuccess(true);
            result.setMessage("Ticket generado correctamente");
        }

        // Verificar si hay fault
        NodeList faultNodes = doc.getElementsByTagName("faultstring");
        if (faultNodes.getLength() > 0) {
            result.setSuccess(false);
            result.setMessage(faultNodes.item(0).getTextContent());
        }

        return result;
    }

    /**
     * Construye el mensaje SOAP XML
     */
    private String buildSoapRequest(String method, String fileName, String contentBase64, EmpresaDTO empresa) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<soapenv:Envelope ");
        sb.append("xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        sb.append("xmlns:ser=\"http://service.sunat.gob.pe\" ");
        sb.append("xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");

        // Header con WS-Security
        sb.append("<soapenv:Header>");
        sb.append("<wsse:Security>");
        sb.append("<wsse:UsernameToken>");
        sb.append("<wsse:Username>").append(empresa.getSolUsername()).append("</wsse:Username>");
        sb.append("<wsse:Password>").append(empresa.getSolClave()).append("</wsse:Password>");
        sb.append("</wsse:UsernameToken>");
        sb.append("</wsse:Security>");
        sb.append("</soapenv:Header>");

        // Body
        sb.append("<soapenv:Body>");
        sb.append("<ser:").append(method).append(">");
        sb.append("<fileName>").append(fileName).append("</fileName>");
        sb.append("<contentFile>").append(contentBase64).append("</contentFile>");
        sb.append("</ser:").append(method).append(">");
        sb.append("</soapenv:Body>");
        sb.append("</soapenv:Envelope>");

        return sb.toString();
    }

    /**
     * Envía el request SOAP por HTTP POST
     */
    private String sendHttpRequest(String soapXml, String soapAction, String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        connection.setRequestProperty("SOAPAction", soapAction);
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        // Enviar request
        try (OutputStream os = connection.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        log.info("SUNAT HTTP Response Code: {}", responseCode);

        // Leer respuesta
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        String response;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            response = sb.toString();
        }

        connection.disconnect();

        if (responseCode >= 400) {
            log.error("Error de SUNAT. Code: {}, Response: {}", responseCode, response);
            throw new RuntimeException("Error SUNAT HTTP " + responseCode + ": " + extractFaultMessage(response));
        }

        return response;
    }

    /**
     * Parsea la respuesta SOAP de SUNAT
     */
    private SunatResponse parseSunatResponse(String responseXml) throws Exception {
        SunatResponse result = new SunatResponse();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(responseXml.getBytes(StandardCharsets.UTF_8)));

        // Buscar el applicationResponse (CDR en Base64)
        NodeList appResponseNodes = doc.getElementsByTagName("applicationResponse");
        if (appResponseNodes.getLength() == 0) {
            appResponseNodes = doc.getElementsByTagName("content");
        }

        if (appResponseNodes.getLength() > 0) {
            String cdrBase64 = appResponseNodes.item(0).getTextContent().trim();
            result.setCdrBase64(cdrBase64);

            // Descomprimir y parsear el CDR
            byte[] cdrZip = Base64.getDecoder().decode(cdrBase64);
            byte[] cdrXml = xmlUtilService.unzip(cdrZip);

            if (cdrXml.length > 0) {
                parseCdr(cdrXml, result);
            }
        }

        // Verificar si hay fault
        NodeList faultNodes = doc.getElementsByTagName("faultstring");
        if (faultNodes.getLength() > 0) {
            result.setSuccess(false);
            result.setDescription(faultNodes.item(0).getTextContent());

            NodeList faultCodeNodes = doc.getElementsByTagName("faultcode");
            if (faultCodeNodes.getLength() > 0) {
                result.setResponseCode(faultCodeNodes.item(0).getTextContent());
            }
        }

        return result;
    }

    /**
     * Parsea el CDR (Constancia de Recepción) de SUNAT
     */
    private void parseCdr(byte[] cdrXml, SunatResponse result) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(cdrXml));

            // ResponseCode
            NodeList responseCodeNodes = doc.getElementsByTagNameNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
                    "ResponseCode");
            if (responseCodeNodes.getLength() > 0) {
                String code = responseCodeNodes.item(0).getTextContent().trim();
                result.setResponseCode(code);
                result.setSuccess("0".equals(code));
            }

            // Description
            NodeList descriptionNodes = doc.getElementsByTagNameNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
                    "Description");
            if (descriptionNodes.getLength() > 0) {
                result.setDescription(descriptionNodes.item(0).getTextContent().trim());
            }

            // Notes (observaciones)
            NodeList noteNodes = doc.getElementsByTagNameNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
                    "Note");
            StringBuilder notes = new StringBuilder();
            for (int i = 0; i < noteNodes.getLength(); i++) {
                if (notes.length() > 0) notes.append(" | ");
                notes.append(noteNodes.item(i).getTextContent().trim());
            }
            result.setNotes(notes.toString());

            log.info("CDR procesado - Código: {}, Descripción: {}", 
                result.getResponseCode(), result.getDescription());

        } catch (Exception e) {
            log.error("Error al parsear CDR: {}", e.getMessage());
            result.setDescription("Error al parsear CDR: " + e.getMessage());
        }
    }

    /**
     * Extrae el mensaje de error de un SOAP Fault
     */
    private String extractFaultMessage(String responseXml) {
        try {
            int start = responseXml.indexOf("<faultstring>");
            int end = responseXml.indexOf("</faultstring>");
            if (start >= 0 && end >= 0) {
                return responseXml.substring(start + "<faultstring>".length(), end);
            }
        } catch (Exception ignored) {}
        return "Error desconocido de SUNAT";
    }

    // ==================== CLASE DE RESPUESTA ====================

    public static class SunatResponse {
        private boolean success;
        private String responseCode;
        private String description;
        private String notes;
        private String cdrBase64;

        // Getters y Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getCdrBase64() { return cdrBase64; }
        public void setCdrBase64(String cdrBase64) { this.cdrBase64 = cdrBase64; }
    }

    /**
     * Respuesta de sendSummary con ticket
     */
    public static class SunatTicketResponse {
        private boolean success;
        private String message;
        private String ticket;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTicket() { return ticket; }
        public void setTicket(String ticket) { this.ticket = ticket; }
    }
}
