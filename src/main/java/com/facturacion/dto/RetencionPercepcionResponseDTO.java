package com.facturacion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RetencionPercepcionResponseDTO {

    private boolean success;
    private String message;

    private String tipo; // "RETENCION" o "PERCEPCION"
    private String serieCorrelativo;
    private String nombreArchivo;

    private BigDecimal totalRetenidoPercibido;
    private BigDecimal totalPagadoCobrado;

    // Respuesta SUNAT
    private String sunatResponseCode;
    private String sunatDescription;
    private String sunatNote;
    private String hashCode;
    private String xmlBase64;
    private String cdrBase64;
}

