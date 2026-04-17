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
     * Envía una guía de remisión via REST a SUNAT GRE.
     * POST /v1/contribuyente/gem/comprobantes/{numRucEmisor}-{codCpe}-{numSerie}-{numCpe}
     */
    public RestResponse sendGuia(String nombreArchivo, byte[] zipContent, EmpresaDTO empresa) {
        String token = oauth2Service.getAccessToken(empresa);
        String endpoint = config.getRestEndpoint(empresa.getAmbiente());
        String url = endpoint + "/comprobantes/" + nombreArchivo;

        log.info("Enviando guía REST a SUNAT: {} → {}", nombreArchivo, url);

        try {
            // La API GRE espera JSON con el ZIP en Base64
            String zipBase64 = Base64.getEncoder().encodeToString(zipContent);
            // El hashZip es SHA-256 del archivo ZIP (bytes originales) en hexadecimal
            String hashZip = generateSha256(zipContent);

            Map<String, Object> archivo = new HashMap<>();
            archivo.put("nomArchivo", nombreArchivo + ".zip");
            archivo.put("arcGreZip", zipBase64);
            archivo.put("hashZip", hashZip);

            Map<String, Object> body = new HashMap<>();
            body.put("archivo", archivo);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            RestResponse result = new RestResponse();

            if (response != null) {
                // SUNAT puede retornar un ticket (procesamiento asíncrono) o CDR directo
                if (response.containsKey("numTicket")) {
                    result.setSuccess(true);
                    result.setTicket((String) response.get("numTicket"));
                    result.setMessage("Guía enviada, ticket: " + result.getTicket());
                } else if (response.containsKey("codRespuesta")) {
                    String code = String.valueOf(response.get("codRespuesta"));
                    result.setSuccess("0".equals(code));
                    result.setResponseCode(code);
                    result.setDescription(response.containsKey("arcCdr")
                            ? "Guía procesada" : String.valueOf(response.get("desRespuesta")));
                    if (response.containsKey("arcCdr")) {
                        result.setCdrBase64((String) response.get("arcCdr"));
                    }
                } else {
                    result.setSuccess(true);
                    result.setMessage("Respuesta recibida de SUNAT");
                }
            }

            log.info("Respuesta REST SUNAT: success={}, ticket={}", result.isSuccess(), result.getTicket());
            return result;

        } catch (WebClientResponseException e) {
            log.error("Error REST SUNAT: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            RestResponse error = new RestResponse();
            error.setSuccess(false);
            error.setMessage("Error SUNAT REST " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            return error;
        } catch (Exception e) {
            log.error("Error al enviar guía REST: {}", e.getMessage());
            RestResponse error = new RestResponse();
            error.setSuccess(false);
            error.setMessage("Error: " + e.getMessage());
            return error;
        }
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
        private String message;
        private String ticket;
        private String responseCode;
        private String description;
        private String cdrBase64;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
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
