package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.EmpresaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente REST para enviar Guías de Remisión a la API GRE de SUNAT.
 * Usa OAuth2 para autenticación.
 */
@Service
public class SunatRestService {

    private static final Logger log = LoggerFactory.getLogger(SunatRestService.class);

    private final SunatConfig config;
    private final SunatOAuth2Service oauth2Service;
    private final WebClient webClient;

    public SunatRestService(SunatConfig config, SunatOAuth2Service oauth2Service) {
        this.config = config;
        this.oauth2Service = oauth2Service;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Consulta el estado de un ticket GRE asíncrono.
     * GET /v1/contribuyente/gem/comprobantes/{numTicket}
     * Devuelve pending=true si SUNAT aún está procesando (numEstado=1).
     */
    public RestResponse consultarTicketGuia(String ticket, EmpresaDTO empresa) {
        String token = oauth2Service.getAccessToken(empresa);
        String baseEndpoint = config.getRestEndpoint(empresa.getAmbiente());
        String url = baseEndpoint + "/comprobantes/envios/" + ticket;

        log.info("=== CONSULTA TICKET GRE ===");
        log.info("  Ambiente    : {}", empresa.getAmbiente());
        log.info("  Base endpoint: {}", baseEndpoint);
        log.info("  URL completa : {}", url);
        log.info("  Ticket      : {}", ticket);
        log.info("  Token (10c) : {}...", token != null && token.length() > 10 ? token.substring(0, 10) : token);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("  Respuesta raw: {}", response);

            RestResponse result = new RestResponse();
            if (response == null) {
                log.warn("  → Respuesta NULL de SUNAT (pending=true)");
                result.setPending(true);
                result.setMessage("Sin respuesta de SUNAT");
                return result;
            }

            log.info("  Claves recibidas: {}", response.keySet());
            for (Map.Entry<String, Object> entry : response.entrySet()) {
                String v = entry.getValue() != null ? entry.getValue().toString() : "null";
                String display = v.length() > 80 ? v.substring(0, 80) + "..." : v;
                log.info("    {} = {}", entry.getKey(), display);
            }

            Object numEstado = response.get("numEstado");
            log.info("  numEstado={}", numEstado);

            if (numEstado != null && "1".equals(String.valueOf(numEstado))) {
                log.info("  → En proceso (numEstado=1), pending=true");
                result.setPending(true);
                result.setMessage("En proceso");
            } else if (response.containsKey("codRespuesta")) {
                String code = String.valueOf(response.get("codRespuesta"));
                // codRespuesta=0 → desRespuesta; codRespuesta=99 → error.desError
                String des = extractDescription(response);
                boolean hasCdr = response.containsKey("arcCdr");
                log.info("  → codRespuesta={}, descripcion={}, arcCdr={}", code, des, hasCdr);
                result.setSuccess("0".equals(code));
                result.setResponseCode(code);
                result.setDescription(des);
                if (hasCdr) {
                    result.setCdrBase64((String) response.get("arcCdr"));
                }
            } else {
                log.warn("  → Respuesta sin numEstado ni codRespuesta — keys: {}", response.keySet());
                result.setPending(true);
                result.setMessage("Respuesta inesperada, reintentando...");
            }
            return result;

        } catch (WebClientResponseException e) {
            log.error("=== ERROR HTTP CONSULTANDO TICKET GRE ===");
            log.error("  URL          : {}", url);
            log.error("  HTTP Status  : {}", e.getStatusCode());
            log.error("  Response body: {}", e.getResponseBodyAsString());
            log.error("  Headers      : {}", e.getHeaders());
            RestResponse error = new RestResponse();
            error.setSuccess(false);
            error.setMessage("Error consultando ticket " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            return error;
        } catch (Exception e) {
            log.error("=== EXCEPCIÓN CONSULTANDO TICKET GRE ===");
            log.error("  URL  : {}", url);
            log.error("  Error: {}", e.getMessage(), e);
            RestResponse error = new RestResponse();
            error.setSuccess(false);
            error.setMessage("Error consultando ticket: " + e.getMessage());
            return error;
        }
    }

    /**
     * Envía una guía de remisión via REST a SUNAT GRE.
     * POST /v1/contribuyente/gem/comprobantes/{numRucEmisor}-{codCpe}-{numSerie}-{numCpe}
     */
    public RestResponse sendGuia(String nombreArchivo, byte[] zipContent, EmpresaDTO empresa) {
        String token = oauth2Service.getAccessToken(empresa);
        String endpoint = config.getRestEndpoint(empresa.getAmbiente());
        String url = endpoint + "/comprobantes/" + nombreArchivo;

        log.info("=== ENVÍO GUÍA GRE REST ===");
        log.info("  URL         : {}", url);
        log.info("  Archivo     : {}", nombreArchivo);
        log.info("  ZIP bytes   : {}", zipContent.length);
        log.info("  Token (10c) : {}...", token != null && token.length() > 10 ? token.substring(0, 10) : token);

        try {
            String zipBase64 = Base64.getEncoder().encodeToString(zipContent);
            String hashZip = generateSha256(zipContent);
            log.info("  hashZip (SHA-256): {}", hashZip);

            Map<String, Object> archivo = new HashMap<>();
            archivo.put("nomArchivo", nombreArchivo + ".zip");
            archivo.put("arcGreZip", zipBase64);
            archivo.put("hashZip", hashZip);

            Map<String, Object> body = new HashMap<>();
            body.put("archivo", archivo);

            log.info("  Body keys   : {}", body.keySet());
            log.info("  archivo.nomArchivo: {}", nombreArchivo + ".zip");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("  Respuesta raw: {}", response);

            RestResponse result = new RestResponse();

            if (response != null) {
                log.info("  Claves recibidas: {}", response.keySet());
                if (response.containsKey("numTicket")) {
                    result.setSuccess(true);
                    result.setPending(true);
                    result.setTicket((String) response.get("numTicket"));
                    result.setMessage("Guía enviada, ticket: " + result.getTicket());
                    log.info("  → Ticket asíncrono: {}", result.getTicket());
                } else if (response.containsKey("codRespuesta")) {
                    String code = String.valueOf(response.get("codRespuesta"));
                    result.setSuccess("0".equals(code));
                    result.setResponseCode(code);
                    result.setDescription(response.containsKey("arcCdr")
                            ? "Guía procesada" : String.valueOf(response.get("desRespuesta")));
                    if (response.containsKey("arcCdr")) {
                        result.setCdrBase64((String) response.get("arcCdr"));
                    }
                    log.info("  → CDR directo: code={}, desc={}", code, result.getDescription());
                } else {
                    result.setSuccess(true);
                    result.setMessage("Respuesta recibida de SUNAT");
                    log.warn("  → Respuesta sin ticket ni codRespuesta, keys: {}", response.keySet());
                }
            } else {
                log.warn("  → Respuesta NULL de SUNAT en sendGuia");
            }

            log.info("  Resultado: success={}, pending={}, ticket={}", result.isSuccess(), result.isPending(), result.getTicket());
            return result;

        } catch (WebClientResponseException e) {
            log.error("=== ERROR HTTP ENVIANDO GUÍA GRE ===");
            log.error("  URL          : {}", url);
            log.error("  HTTP Status  : {}", e.getStatusCode());
            log.error("  Response body: {}", e.getResponseBodyAsString());
            log.error("  Headers      : {}", e.getHeaders());
            RestResponse error = new RestResponse();
            error.setSuccess(false);
            error.setMessage("Error SUNAT REST " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            return error;
        } catch (Exception e) {
            log.error("=== EXCEPCIÓN ENVIANDO GUÍA GRE ===");
            log.error("  Error: {}", e.getMessage(), e);
            RestResponse error = new RestResponse();
            error.setSuccess(false);
            error.setMessage("Error: " + e.getMessage());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> response) {
        Object des = response.get("desRespuesta");
        if (des != null && !"null".equals(des.toString())) {
            return des.toString();
        }
        Object errorObj = response.get("error");
        if (errorObj instanceof Map) {
            Map<String, Object> error = (Map<String, Object>) errorObj;
            return "Error " + error.get("numError") + ": " + error.get("desError");
        }
        return null;
    }

    private String generateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            // Retornar en formato hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generando hash SHA-256", e);
        }
    }

    public static class RestResponse {
        private boolean success;
        private boolean pending;
        private String message;
        private String ticket;
        private String responseCode;
        private String description;
        private String cdrBase64;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public boolean isPending() { return pending; }
        public void setPending(boolean pending) { this.pending = pending; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTicket() { return ticket; }
        public void setTicket(String ticket) { this.ticket = ticket; }
        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCdrBase64() { return cdrBase64; }
        public void setCdrBase64(String cdrBase64) { this.cdrBase64 = cdrBase64; }
    }
}
