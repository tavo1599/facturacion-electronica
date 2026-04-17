package com.facturacion.controller;

import com.facturacion.dto.GuiaRemisionRequestDTO;
import com.facturacion.dto.GuiaRemisionResponseDTO;
import com.facturacion.service.GuiaRemisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guias")
@CrossOrigin(origins = "*")
@Tag(name = "Guías de Remisión", description = "Emisión de Guías de Remisión Electrónica")
public class GuiaRemisionController {

    private static final Logger log = LoggerFactory.getLogger(GuiaRemisionController.class);
    private final GuiaRemisionService guiaService;

    public GuiaRemisionController(GuiaRemisionService guiaService) {
        this.guiaService = guiaService;
    }

    @Operation(summary = "Emitir guía de remisión",
            description = "Genera XML UBL 2.1, firma, comprime y envía a SUNAT")
    @PostMapping("/emitir")
    public ResponseEntity<GuiaRemisionResponseDTO> emitirGuia(
            @Valid @RequestBody GuiaRemisionRequestDTO request) {

        log.info(">>> Guía de remisión: {} {}", request.getTipoGuia(), request.getSerieCorrelativo());

        GuiaRemisionResponseDTO response = guiaService.emitirGuia(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }
}

