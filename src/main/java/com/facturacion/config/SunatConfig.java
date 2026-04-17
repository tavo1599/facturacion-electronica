package com.facturacion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración global SUNAT.
 * Solo contiene endpoints y rutas base de almacenamiento/certificados.
 * Los datos de empresa, SOL y certificado viajan en cada request (multiempresa).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sunat")
public class SunatConfig {

    private Endpoints endpoints = new Endpoints();
    private String certificatesBasePath = "./certificates";
    private Gre gre = new Gre();

    @Data
    public static class Endpoints {
        // ...existing fields...
        private String beta;
        private String produccion;
        private String cpegemBeta;
        private String cpegemProduccion;
        private String guiaBeta;
        private String guiaProduccion;
        private String restBeta;
        private String restProduccion;
        private String oauth2Beta;
        private String oauth2Produccion;
    }

    @Data
    public static class Gre {
        private GreAmbiente beta = new GreAmbiente();
        private GreAmbiente produccion = new GreAmbiente();
    }

    @Data
    public static class GreAmbiente {
        private String clientId = "";
        private String clientSecret = "";
    }

    /**
     * Retorna las credenciales GRE (OAuth2) para un ambiente
     */
    public GreAmbiente getGreCredentials(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? gre.getProduccion() : gre.getBeta();
    }

    /**
     * Retorna la URL del web service SOAP principal según el ambiente
     */
    public String getEndpoint(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente)
            ? endpoints.getProduccion()
            : endpoints.getBeta();
    }

    /**
     * Retorna el endpoint SOAP para Retenciones/Percepciones
     */
    public String getCpeGemEndpoint(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente)
            ? endpoints.getCpegemProduccion()
            : endpoints.getCpegemBeta();
    }

    /**
     * Retorna el endpoint REST para Guías de Remisión
     */
    public String getRestEndpoint(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente)
            ? endpoints.getRestProduccion()
            : endpoints.getRestBeta();
    }

    /**
     * Retorna el endpoint SOAP para Guías de Remisión
     */
    public String getGuiaEndpoint(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente)
            ? endpoints.getGuiaProduccion()
            : endpoints.getGuiaBeta();
    }

    /**
     * Retorna el endpoint OAuth2 para obtener token
     */
    public String getOAuth2Endpoint(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente)
            ? endpoints.getOauth2Produccion()
            : endpoints.getOauth2Beta();
    }

    /**
     * Ruta del certificado para un RUC específico y ambiente
     */
    public String getCertificatePath(String ruc, String ambiente) {
        return certificatesBasePath + "/" + resolveAmbiente(ambiente) + "/" + ruc + "/certificate.pfx";
    }

    /**
     * Ruta del certificado (usa ambiente beta por defecto)
     */
    public String getCertificatePath(String ruc) {
        return getCertificatePath(ruc, "beta");
    }

    /**
     * Ruta del archivo de contraseña del certificado para un ambiente
     */
    public String getCertificatePasswordPath(String ruc, String ambiente) {
        return certificatesBasePath + "/" + resolveAmbiente(ambiente) + "/" + ruc + "/certificate.password";
    }

    /**
     * Ruta del archivo de contraseña (usa ambiente beta por defecto)
     */
    public String getCertificatePasswordPath(String ruc) {
        return getCertificatePasswordPath(ruc, "beta");
    }

    /**
     * Directorio base de certificados para un RUC y ambiente
     */
    public String getCertificateDir(String ruc, String ambiente) {
        return certificatesBasePath + "/" + resolveAmbiente(ambiente) + "/" + ruc;
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}
