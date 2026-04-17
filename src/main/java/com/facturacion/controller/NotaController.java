package com.facturacion.controller;

import com.facturacion.dto.NotaRequestDTO;
import com.facturacion.dto.NotaResponseDTO;
import com.facturacion.service.NotaService;
import com.facturacion.util.SunatValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para Notas de Crédito y Débito - SUNAT UBL 2.1
 */
@RestController
@RequestMapping("/api/notas")
@CrossOrigin(origins = "*")
@Tag(name = "Notas de Crédito/Débito", description = "Emisión de Notas de Crédito y Débito")
public class NotaController {

    private static final Logger log = LoggerFactory.getLogger(NotaController.class);

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    /**
     * EMITIR NOTA DE CRÉDITO O DÉBITO
     * Genera XML UBL 2.1, firma, comprime y envía a SUNAT
     *
     * POST /api/notas/emitir
     */
    @Operation(summary = "Emitir Nota de Crédito o Débito", description = "Genera XML, firma, comprime y envía a SUNAT")
    @PostMapping("/emitir")
    public ResponseEntity<NotaResponseDTO> emitirNota(
            @Valid @RequestBody NotaRequestDTO request) {

        String tipo = "07".equals(request.getTipoNota()) ? "NOTA DE CRÉDITO" : "NOTA DE DÉBITO";
        log.info(">>> Solicitud de emisión: {} {}", tipo, request.getSerieCorrelativo());

        validarNota(request);

        NotaResponseDTO response = notaService.emitirNota(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }

    /**
     * GENERAR XML SIN ENVIAR
     *
     * POST /api/notas/generar-xml
     */
    @Operation(summary = "Generar XML de nota sin enviar", description = "Genera XML UBL 2.1 de la nota sin enviar a SUNAT")
    @PostMapping("/generar-xml")
    public ResponseEntity<NotaResponseDTO> generarXml(
            @Valid @RequestBody NotaRequestDTO request) {

        log.info(">>> Generación XML nota: {}", request.getSerieCorrelativo());

        validarNota(request);

        NotaResponseDTO response = notaService.generarXmlSinEnviar(request);
        return ResponseEntity.ok(response);
    }

    // ==================== VALIDACIONES ====================

    private void validarNota(NotaRequestDTO request) {
        // Validación completa según normativa SUNAT
        java.util.List<String> errores = SunatValidator.validarNota(request);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(
                    "Errores de validación SUNAT: " + String.join("; ", errores));
        }
    }
}
