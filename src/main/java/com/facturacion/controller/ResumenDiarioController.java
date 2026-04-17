package com.facturacion.controller;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.ResumenBajaResponseDTO;
import com.facturacion.dto.ResumenDiarioRequestDTO;
import com.facturacion.service.ResumenDiarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resumenes")
@CrossOrigin(origins = "*")
@Tag(name = "Resumen Diario", description = "Resumen diario de boletas de venta para SUNAT")
public class ResumenDiarioController {

    private static final Logger log = LoggerFactory.getLogger(ResumenDiarioController.class);
    private final ResumenDiarioService resumenService;

    public ResumenDiarioController(ResumenDiarioService resumenService) {
        this.resumenService = resumenService;
    }

    @Operation(summary = "Enviar resumen diario de boletas",
            description = "Genera XML de resumen, firma, comprime y envía a SUNAT. Retorna un ticket.")
    @PostMapping("/enviar")
    public ResponseEntity<ResumenBajaResponseDTO> enviarResumen(
            @Valid @RequestBody ResumenDiarioRequestDTO request) {

        log.info(">>> Resumen diario: {}", request.getIdentificador());

        ResumenBajaResponseDTO response = resumenService.enviarResumen(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }

    @Operation(summary = "Consultar estado de resumen por ticket",
            description = "Consulta a SUNAT el estado del procesamiento de un resumen diario")
    @PostMapping("/estado")
    public ResponseEntity<ResumenBajaResponseDTO> consultarEstado(
            @Parameter(description = "Número de ticket", example = "1681234567890") @RequestParam String ticket,
            @Valid @RequestBody EmpresaDTO empresa) {

        log.info(">>> Consulta estado resumen - Ticket: {}", ticket);

        ResumenBajaResponseDTO response = resumenService.consultarEstado(ticket, empresa);
        return ResponseEntity.ok(response);
    }
}

