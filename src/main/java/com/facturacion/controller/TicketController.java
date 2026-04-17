package com.facturacion.controller;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.PendingTicket;
import com.facturacion.dto.ResumenBajaResponseDTO;
import com.facturacion.service.BajaService;
import com.facturacion.service.ResumenDiarioService;
import com.facturacion.service.TicketStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
@Tag(name = "Tickets", description = "Gestión y consulta de tickets de Bajas y Resúmenes")
public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketController.class);

    private final TicketStore ticketStore;
    private final BajaService bajaService;
    private final ResumenDiarioService resumenService;

    public TicketController(TicketStore ticketStore, BajaService bajaService,
                            ResumenDiarioService resumenService) {
        this.ticketStore = ticketStore;
        this.bajaService = bajaService;
        this.resumenService = resumenService;
    }

    @Operation(summary = "Listar tickets pendientes",
            description = "Muestra todos los tickets que aún están siendo procesados por SUNAT")
    @GetMapping("/pendientes")
    public ResponseEntity<List<PendingTicket>> listarPendientes() {
        return ResponseEntity.ok(ticketStore.getPendingTickets());
    }

    @Operation(summary = "Listar todos los tickets",
            description = "Muestra todos los tickets registrados (pendientes, completados, fallidos)")
    @GetMapping
    public ResponseEntity<List<PendingTicket>> listarTodos() {
        return ResponseEntity.ok(ticketStore.getAllTickets());
    }

    @Operation(summary = "Ver estado de un ticket",
            description = "Consulta el estado actual de un ticket específico")
    @GetMapping("/{ticket}")
    public ResponseEntity<?> verTicket(
            @Parameter(description = "Número de ticket", example = "1681234567890") @PathVariable String ticket) {
        PendingTicket pt = ticketStore.getTicket(ticket);
        if (pt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pt);
    }

    @Operation(summary = "Consultar ticket manualmente en SUNAT",
            description = "Fuerza una consulta a SUNAT del estado de un ticket (sin esperar al polling)")
    @PostMapping("/{ticket}/consultar")
    public ResponseEntity<ResumenBajaResponseDTO> consultarManual(
            @Parameter(description = "Número de ticket") @PathVariable String ticket,
            @Valid @RequestBody EmpresaDTO empresa) {

        log.info(">>> Consulta manual ticket: {}", ticket);

        PendingTicket pt = ticketStore.getTicket(ticket);
        String tipo = (pt != null) ? pt.getTipo() : "BAJA";

        ResumenBajaResponseDTO response;
        if ("RESUMEN".equals(tipo)) {
            response = resumenService.consultarEstado(ticket, empresa);
        } else {
            response = bajaService.consultarEstado(ticket, empresa);
        }

        // Actualizar estado en store si completó
        if (response.isSuccess() && pt != null) {
            ticketStore.updateTicket(ticket, "COMPLETED");
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar un ticket del registro")
    @DeleteMapping("/{ticket}")
    public ResponseEntity<Map<String, Object>> eliminarTicket(
            @Parameter(description = "Número de ticket") @PathVariable String ticket) {
        ticketStore.removeTicket(ticket);
        return ResponseEntity.ok(Map.of("success", true, "message", "Ticket eliminado: " + ticket));
    }
}

