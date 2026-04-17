package com.facturacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingTicket {

    private String ticket;
    private String tipo; // BAJA, RESUMEN
    private String nombreArchivo;
    private String ruc;
    private String ambiente;

    // Datos empresa serializados para poder consultar getStatus
    private EmpresaDTO empresa;

    private LocalDateTime createdAt;
    private LocalDateTime lastChecked;
    private int retryCount;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
}

