package com.facturacion.controller;

import com.facturacion.dto.PercepcionRequestDTO;
import com.facturacion.dto.RetencionPercepcionResponseDTO;
import com.facturacion.service.PercepcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/percepciones")
@CrossOrigin(origins = "*")
@Tag(name = "Percepciones", description = "Emisión de Comprobantes de Percepción (tipo 40)")
public class PercepcionController {

    private static final Logger log = LoggerFactory.getLogger(PercepcionController.class);
    private final PercepcionService percepcionService;

    public PercepcionController(PercepcionService percepcionService) {
        this.percepcionService = percepcionService;
    }

    @Operation(summary = "Emitir comprobante de percepción",
            description = "Genera XML, firma, comprime y envía a SUNAT (endpoint CPE-GEM SFS)")
    @PostMapping("/emitir")
    public ResponseEntity<RetencionPercepcionResponseDTO> emitirPercepcion(
            @Valid @RequestBody PercepcionRequestDTO request) {

        log.info(">>> Percepción: {}", request.getSerieCorrelativo());

        RetencionPercepcionResponseDTO response = percepcionService.emitirPercepcion(request);

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.unprocessableEntity().body(response);
    }
}

