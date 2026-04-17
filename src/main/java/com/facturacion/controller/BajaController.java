package com.facturacion.controller;

import com.facturacion.dto.BajaRequestDTO;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.ResumenBajaResponseDTO;
import com.facturacion.service.BajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bajas")
@CrossOrigin(origins = "*")
@Tag(name = "Comunicación de Baja", description = "Anulación de comprobantes electrónicos ante SUNAT")
public class BajaController {

    private static final Logger log = LoggerFactory.getLogger(BajaController.class);
    private final BajaService bajaService;

    public BajaController(BajaService bajaService) {
        this.bajaService = bajaService;
    }

    @Operation(summary = "Enviar comunicación de baja",
            description = "Genera XML de baja, firma, comprime y envía a SUNAT. Retorna un ticket para consultar el resultado.")
    @PostMapping("/enviar")
    public ResponseEntity<ResumenBajaResponseDTO> enviarBaja(
            @Valid @RequestBody BajaRequestDTO request) {

        log.info(">>> Comunicación de baja: {}", request.getIdentificador());

        ResumenBajaResponseDTO response = bajaService.enviarBaja(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }

    @Operation(summary = "Consultar estado de baja por ticket",
            description = "Consulta a SUNAT el estado del procesamiento de una comunicación de baja")
    @PostMapping("/estado")
    public ResponseEntity<ResumenBajaResponseDTO> consultarEstado(
            @Parameter(description = "Número de ticket", example = "1681234567890") @RequestParam String ticket,
            @Valid @RequestBody EmpresaDTO empresa) {

        log.info(">>> Consulta estado baja - Ticket: {}", ticket);

        ResumenBajaResponseDTO response = bajaService.consultarEstado(ticket, empresa);
        return ResponseEntity.ok(response);
    }
}

