package com.facturacion.controller;

import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.ComprobanteResponseDTO;
import com.facturacion.service.FacturacionService;
import com.facturacion.util.SunatValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API REST para Facturación Electrónica SUNAT - UBL 2.1
 */
@RestController
@RequestMapping("/api/comprobantes")
@CrossOrigin(origins = "*")
@Tag(name = "Comprobantes", description = "Emisión de Facturas y Boletas de Venta")
public class ComprobanteController {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteController.class);

    private final FacturacionService facturacionService;

    public ComprobanteController(FacturacionService facturacionService) {
        this.facturacionService = facturacionService;
    }

    /**
     * EMITIR COMPROBANTE (Factura o Boleta)
     * Genera XML UBL 2.1, firma, comprime y envía a SUNAT
     * 
     * POST /api/comprobantes/emitir
     */
    @Operation(summary = "Emitir comprobante", description = "Genera XML UBL 2.1, firma digitalmente, comprime en ZIP y envía a SUNAT")
    @PostMapping("/emitir")
    public ResponseEntity<ComprobanteResponseDTO> emitirComprobante(
            @Valid @RequestBody ComprobanteRequestDTO request) {

        log.info(">>> Solicitud de emisión recibida: {} {}", 
            "01".equals(request.getTipoComprobante()) ? "FACTURA" : "BOLETA",
            request.getSerieCorrelativo());

        // Validaciones adicionales
        validarComprobante(request);

        ComprobanteResponseDTO response = facturacionService.emitirComprobante(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }

    /**
     * GENERAR XML SIN ENVIAR
     * Útil para revisión y pruebas antes de enviar a SUNAT
     * 
     * POST /api/comprobantes/generar-xml
     */
    @Operation(summary = "Generar XML sin enviar", description = "Genera el XML UBL 2.1 sin firmar ni enviar a SUNAT (para revisión)")
    @PostMapping("/generar-xml")
    public ResponseEntity<ComprobanteResponseDTO> generarXml(
            @Valid @RequestBody ComprobanteRequestDTO request) {

        log.info(">>> Solicitud de generación XML: {}", request.getSerieCorrelativo());

        validarComprobante(request);

        ComprobanteResponseDTO response = facturacionService.generarXmlSinEnviar(request);

        return ResponseEntity.ok(response);
    }

    /**
     * EMITIR LOTE DE COMPROBANTES
     * Procesa varios comprobantes en secuencia
     * 
     * POST /api/comprobantes/emitir-lote
     */
    @Operation(summary = "Emitir lote de comprobantes", description = "Procesa múltiples comprobantes en secuencia")
    @PostMapping("/emitir-lote")
    public ResponseEntity<List<ComprobanteResponseDTO>> emitirLote(
            @Valid @RequestBody List<ComprobanteRequestDTO> requests) {

        log.info(">>> Solicitud de emisión en lote: {} comprobantes", requests.size());

        List<ComprobanteResponseDTO> responses = requests.stream()
                .map(request -> {
                    try {
                        validarComprobante(request);
                        return facturacionService.emitirComprobante(request);
                    } catch (IllegalArgumentException e) {
                        return ComprobanteResponseDTO.builder()
                                .success(false)
                                .message("Error de validación: " + e.getMessage())
                                .serieCorrelativo(request.getSerieCorrelativo())
                                .build();
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * HEALTH CHECK
     * GET /api/comprobantes/health
     */
    @Operation(summary = "Health check", description = "Verifica que el sistema esté activo")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Facturación Electrónica SUNAT - UBL 2.1 - Sistema activo");
    }

    // ==================== VALIDACIONES ====================

    private void validarComprobante(ComprobanteRequestDTO request) {
        // Validación completa según normativa SUNAT
        java.util.List<String> errores = SunatValidator.validarComprobante(request);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(
                "Errores de validación SUNAT: " + String.join("; ", errores));
        }

        // Validar que los items tengan valores coherentes
        request.getItems().forEach(item -> {
            if (item.getValorUnitario().compareTo(item.getPrecioUnitario()) > 0) {
                log.warn("Item {}: valorUnitario ({}) es mayor que precioUnitario ({}). " +
                    "Verifique que precioUnitario incluya IGV.",
                    item.getNumero(), item.getValorUnitario(), item.getPrecioUnitario());
            }
        });
    }
}
