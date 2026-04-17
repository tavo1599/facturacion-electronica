package com.facturacion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComprobanteResponseDTO {

    private boolean success;
    private String message;

    // Datos del comprobante
    private String tipoComprobante;
    private String serieCorrelativo;
    private String nombreArchivo;

    // Totales
    private BigDecimal totalGravado;
    private BigDecimal totalExonerado;
    private BigDecimal totalInafecto;
    private BigDecimal totalIgv;
    private BigDecimal importeTotal;

    // Respuesta SUNAT
    private String sunatResponseCode;
    private String sunatDescription;
    private String sunatNote;           // Observaciones
    private String hashCode;            // Hash del XML
    private String xmlBase64;           // XML firmado en Base64

    // CDR
    private String cdrBase64;           // CDR en Base64 (ZIP)
}
