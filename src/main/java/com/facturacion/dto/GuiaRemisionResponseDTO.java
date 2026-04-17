package com.facturacion.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuiaRemisionResponseDTO {

    private boolean success;
    private String message;

    private String tipoGuia;
    private String serieCorrelativo;
    private String nombreArchivo;

    // Respuesta SUNAT
    private String sunatResponseCode;
    private String sunatDescription;
    private String sunatNote;
    private String hashCode;
    private String xmlBase64;
    private String cdrBase64;
}

