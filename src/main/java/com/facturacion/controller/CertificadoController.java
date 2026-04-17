package com.facturacion.controller;

import com.facturacion.config.SunatConfig;
import com.facturacion.service.SignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * API para gestión de certificados digitales por empresa (RUC) y ambiente.
 */
@RestController
@RequestMapping("/api/certificados")
@CrossOrigin(origins = "*")
@Tag(name = "Certificados", description = "Gestión de certificados digitales (.pfx/.p12) por empresa y ambiente")
public class CertificadoController {

    private static final Logger log = LoggerFactory.getLogger(CertificadoController.class);

    private final SunatConfig config;
    private final SignatureService signatureService;

    public CertificadoController(SunatConfig config, SignatureService signatureService) {
        this.config = config;
        this.signatureService = signatureService;
    }

    /**
     * Sube un certificado digital (.pfx/.p12) para una empresa.
     *
     * POST /api/certificados/{ruc}/upload
     * Content-Type: multipart/form-data
     *   - archivo: archivo .pfx o .p12
     *   - password: contraseña del certificado
     */
    @Operation(summary = "Subir certificado digital", description = "Sube un certificado .pfx/.p12 y su contraseña para una empresa y ambiente")
    @PostMapping("/{ruc}/upload")
    public ResponseEntity<Map<String, Object>> uploadCertificado(
            @Parameter(description = "RUC del emisor (11 dígitos)", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Archivo .pfx o .p12") @RequestParam("archivo") MultipartFile archivo,
            @Parameter(description = "Contraseña del certificado", example = "123456") @RequestParam("password") String password,
            @Parameter(description = "Ambiente: beta o produccion", example = "beta") @RequestParam(value = "ambiente", defaultValue = "beta") String ambiente) {

        if (!ruc.matches("\\d{11}")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "RUC inválido. Debe tener 11 dígitos."
            ));
        }

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El archivo del certificado es obligatorio."
            ));
        }

        try {
            // Crear directorio del RUC por ambiente
            String certDir = config.getCertificateDir(ruc, ambiente);
            new File(certDir).mkdirs();

            // Guardar certificado
            String certPath = config.getCertificatePath(ruc, ambiente);
            try (FileOutputStream fos = new FileOutputStream(certPath)) {
                fos.write(archivo.getBytes());
            }

            // Guardar contraseña
            String passwordPath = config.getCertificatePasswordPath(ruc, ambiente);
            Files.writeString(Path.of(passwordPath), password);

            // Invalidar caché para que se recargue
            signatureService.invalidateCache(ruc, ambiente);

            log.info("Certificado subido para RUC: {} ({} bytes)", ruc, archivo.getSize());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Certificado subido correctamente para RUC " + ruc + " en ambiente " + ambiente,
                    "ruc", ruc,
                    "ambiente", ambiente,
                    "path", certPath
            ));

        } catch (Exception e) {
            log.error("Error al subir certificado para RUC {}: {}", ruc, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error al guardar certificado: " + e.getMessage()
            ));
        }
    }

    /**
     * Verifica si un RUC tiene certificado configurado.
     *
     * GET /api/certificados/{ruc}/status
     */
    @Operation(summary = "Verificar certificado", description = "Verifica si un RUC tiene certificado configurado en un ambiente")
    @GetMapping("/{ruc}/status")
    public ResponseEntity<Map<String, Object>> verificarCertificado(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Ambiente: beta o produccion", example = "beta") @RequestParam(value = "ambiente", defaultValue = "beta") String ambiente) {
        String certPath = config.getCertificatePath(ruc, ambiente);
        boolean exists = new File(certPath).exists();

        return ResponseEntity.ok(Map.of(
                "ruc", ruc,
                "ambiente", ambiente,
                "certificadoConfigurado", exists,
                "path", certPath
        ));
    }
}

