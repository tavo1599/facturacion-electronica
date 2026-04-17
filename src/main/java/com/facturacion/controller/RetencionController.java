package com.facturacion.controller;

import com.facturacion.dto.RetencionPercepcionResponseDTO;
import com.facturacion.dto.RetencionRequestDTO;
import com.facturacion.service.RetencionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/retenciones")
@CrossOrigin(origins = "*")
@Tag(name = "Retenciones", description = "Emisión de Comprobantes de Retención (tipo 20)")
public class RetencionController {

    private static final Logger log = LoggerFactory.getLogger(RetencionController.class);
    private final RetencionService retencionService;

    public RetencionController(RetencionService retencionService) {
        this.retencionService = retencionService;
    }

    @Operation(summary = "Emitir comprobante de retención",
            description = "Genera XML, firma, comprime y envía a SUNAT (endpoint CPE-GEM SFS)")
    @PostMapping("/emitir")
    public ResponseEntity<RetencionPercepcionResponseDTO> emitirRetencion(
            @Valid @RequestBody RetencionRequestDTO request) {

        log.info(">>> Retención: {}", request.getSerieCorrelativo());

        RetencionPercepcionResponseDTO response = retencionService.emitirRetencion(request);

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.unprocessableEntity().body(response);
    }
}

