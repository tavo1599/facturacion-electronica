package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Respuesta de Comunicación de Baja o Resumen Diario")
public class ResumenBajaResponseDTO {

    private boolean success;
    private String message;

    private String tipo; // "BAJA" o "RESUMEN"
    private String identificador; // RA-20260416-1 o RC-20260416-1
    private String nombreArchivo;

    // Ticket SUNAT (para consultar estado después)
    private String ticket;

    // Estado (cuando se consulta con getStatus)
    private String sunatResponseCode;
    private String sunatDescription;
    private String sunatNote;
    private String cdrBase64;
    private String xmlBase64;
}

