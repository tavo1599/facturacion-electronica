package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.EmpresaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona tokens OAuth2 para la API REST de SUNAT (Guías de Remisión GRE).
 * Cachea tokens por RUC+ambiente con expiración automática.
 */
@Service
public class SunatOAuth2Service {

    private static final Logger log = LoggerFactory.getLogger(SunatOAuth2Service.class);

    private final SunatConfig config;
    private final WebClient webClient;
    private final ConcurrentHashMap<String, TokenHolder> tokenCache = new ConcurrentHashMap<>();

    public SunatOAuth2Service(SunatConfig config) {
        this.config = config;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Obtiene un access token válido para la empresa.
     * Beta: usa grant_type=password con credenciales Nubefact test.
     * Producción: usa grant_type=password con credenciales SOL reales.
     */
    public String getAccessToken(EmpresaDTO empresa) {
        String cacheKey = empresa.getRuc() + ":" + empresa.getAmbiente();

        TokenHolder cached = tokenCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.accessToken;
        }

        // Obtener credenciales GRE del yml o del request
        SunatConfig.GreAmbiente greCreds = config.getGreCredentials(empresa.getAmbiente());
        String clientId = (empresa.getClientId() != null && !empresa.getClientId().isBlank())
                ? empresa.getClientId() : greCreds.getClientId();
        String clientSecret = (empresa.getClientSecret() != null && !empresa.getClientSecret().isBlank())
                ? empresa.getClientSecret() : greCreds.getClientSecret();

        String tokenUrl = config.getOAuth2Endpoint(empresa.getAmbiente())
                + "/" + clientId + "/oauth2/token";

        // Username SOL: RUC + usuario SOL
        String username = empresa.getRuc() + empresa.getSolUsuario();
        String password = empresa.getSolClave();

        log.info("Solicitando token OAuth2 GRE para RUC: {} en {}", empresa.getRuc(), tokenUrl);
        log.info("Username SOL: {} | ClientId: {}", username, clientId);

        try {
            // Construir body manualmente para asegurar Content-Type correcto
            String formBody = "grant_type=password"
                    + "&scope=" + URLEncoder.encode("https://api-cpe.sunat.gob.pe", StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("access_token")) {
                String token = (String) response.get("access_token");
                int expiresIn = response.containsKey("expires_in")
                        ? ((Number) response.get("expires_in")).intValue() : 3600;

                tokenCache.put(cacheKey, new TokenHolder(token, expiresIn));
                log.info("Token OAuth2 GRE obtenido para RUC: {} (expira en {} seg)", empresa.getRuc(), expiresIn);
                return token;
            }

            throw new RuntimeException("Respuesta OAuth2 sin access_token: " + response);

        } catch (Exception e) {
            log.error("Error al obtener token OAuth2 GRE: {}", e.getMessage());
            throw new RuntimeException("Error OAuth2 SUNAT: " + e.getMessage(), e);
        }
    }

    /**
     * Invalida el token cacheado para un RUC
     */
    public void invalidateToken(String ruc, String ambiente) {
        tokenCache.remove(ruc + ":" + ambiente);
    }

    private static class TokenHolder {
        final String accessToken;
        final long expiresAt;

        TokenHolder(String accessToken, int expiresInSeconds) {
            this.accessToken = accessToken;
            // Restar 60 segundos de margen
            this.expiresAt = System.currentTimeMillis() + (expiresInSeconds - 60) * 1000L;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}

