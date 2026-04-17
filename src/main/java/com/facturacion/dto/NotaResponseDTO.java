package com.facturacion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NotaResponseDTO {

    private boolean success;
    private String message;

    // Datos de la nota
    private String tipoNota;
    private String serieCorrelativo;
    private String nombreArchivo;

    // Documento afectado
    private String tipoComprobanteAfectado;
    private String comprobanteAfectado;
    private String codigoMotivo;
    private String descripcionMotivo;

    // Totales
    private BigDecimal totalGravado;
    private BigDecimal totalExonerado;
    private BigDecimal totalInafecto;
    private BigDecimal totalIgv;
    private BigDecimal importeTotal;

    // Respuesta SUNAT
    private String sunatResponseCode;
    private String sunatDescription;
    private String sunatNote;
    private String hashCode;
    private String xmlBase64;
    private String cdrBase64;
}
