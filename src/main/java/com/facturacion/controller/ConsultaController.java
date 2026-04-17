package com.facturacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

/**
 * API REST para consulta y descarga de archivos generados:
 * XML firmados, ZIP comprimidos y CDR de SUNAT.
 *
 * Consulta por: RUC + tipo comprobante + serie + número + ambiente
 */
@RestController
@RequestMapping("/api/consulta")
@CrossOrigin(origins = "*")
@Tag(name = "Consulta de Archivos", description = "Descarga de XML, ZIP y CDR generados por la facturación")
public class ConsultaController {

    private static final Logger log = LoggerFactory.getLogger(ConsultaController.class);

    @Value("${storage.xml-path:./generated-xml}")
    private String xmlBasePath;

    @Value("${storage.zip-path:./generated-zip}")
    private String zipBasePath;

    @Value("${storage.cdr-path:./received-cdr}")
    private String cdrBasePath;

    @Value("${storage.bajas-path:./generated-bajas}")
    private String bajasBasePath;

    @Value("${storage.resumenes-path:./generated-resumenes}")
    private String resumenesBasePath;

    @Value("${storage.guias-path:./generated-guias}")
    private String guiasBasePath;

    @Value("${storage.retenciones-path:./generated-retenciones}")
    private String retencionesBasePath;

    @Value("${storage.percepciones-path:./generated-percepciones}")
    private String percepcionesBasePath;

    // ==================== XML ====================

    @Operation(summary = "Descargar XML firmado",
            description = "Descarga el archivo XML firmado de un comprobante emitido")
    @GetMapping("/{ruc}/xml/{tipoComprobante}/{serie}/{numero}")
    public ResponseEntity<?> descargarXml(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Tipo: 01=Factura, 03=Boleta, 07=NC, 08=ND", example = "01") @PathVariable String tipoComprobante,
            @Parameter(description = "Serie del comprobante", example = "F001") @PathVariable String serie,
            @Parameter(description = "Número correlativo", example = "1") @PathVariable int numero,
            @Parameter(description = "Ambiente: beta o produccion", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = buildNombreArchivo(ruc, tipoComprobante, serie, numero);
        String ambienteResuelto = resolveAmbiente(ambiente);
        String filePath = xmlBasePath + "/" + ambienteResuelto + "/" + ruc + "/" + nombreArchivo + ".xml";

        return descargarArchivo(filePath, nombreArchivo + ".xml", MediaType.APPLICATION_XML);
    }

    // ==================== ZIP ====================

    @Operation(summary = "Descargar ZIP",
            description = "Descarga el archivo ZIP enviado a SUNAT")
    @GetMapping("/{ruc}/zip/{tipoComprobante}/{serie}/{numero}")
    public ResponseEntity<?> descargarZip(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Tipo: 01=Factura, 03=Boleta, 07=NC, 08=ND", example = "01") @PathVariable String tipoComprobante,
            @Parameter(description = "Serie del comprobante", example = "F001") @PathVariable String serie,
            @Parameter(description = "Número correlativo", example = "1") @PathVariable int numero,
            @Parameter(description = "Ambiente: beta o produccion", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = buildNombreArchivo(ruc, tipoComprobante, serie, numero);
        String ambienteResuelto = resolveAmbiente(ambiente);
        String filePath = zipBasePath + "/" + ambienteResuelto + "/" + ruc + "/" + nombreArchivo + ".zip";

        return descargarArchivo(filePath, nombreArchivo + ".zip",
                MediaType.parseMediaType("application/zip"));
    }

    // ==================== CDR ====================

    @Operation(summary = "Descargar CDR (Constancia de Recepción)",
            description = "Descarga el CDR recibido de SUNAT para un comprobante")
    @GetMapping("/{ruc}/cdr/{tipoComprobante}/{serie}/{numero}")
    public ResponseEntity<?> descargarCdr(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Tipo: 01=Factura, 03=Boleta, 07=NC, 08=ND", example = "01") @PathVariable String tipoComprobante,
            @Parameter(description = "Serie del comprobante", example = "F001") @PathVariable String serie,
            @Parameter(description = "Número correlativo", example = "1") @PathVariable int numero,
            @Parameter(description = "Ambiente: beta o produccion", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = buildNombreArchivo(ruc, tipoComprobante, serie, numero);
        String ambienteResuelto = resolveAmbiente(ambiente);
        String filePath = cdrBasePath + "/" + ambienteResuelto + "/" + ruc + "/R-" + nombreArchivo + ".zip";

        return descargarArchivo(filePath, "R-" + nombreArchivo + ".zip",
                MediaType.parseMediaType("application/zip"));
    }

    // ==================== VERIFICAR EXISTENCIA ====================

    @Operation(summary = "Verificar archivos de un comprobante",
            description = "Verifica si existen los archivos XML, ZIP y CDR de un comprobante")
    @GetMapping("/{ruc}/existe/{tipoComprobante}/{serie}/{numero}")
    public ResponseEntity<Map<String, Object>> verificarArchivos(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Tipo: 01=Factura, 03=Boleta, 07=NC, 08=ND", example = "01") @PathVariable String tipoComprobante,
            @Parameter(description = "Serie del comprobante", example = "F001") @PathVariable String serie,
            @Parameter(description = "Número correlativo", example = "1") @PathVariable int numero,
            @Parameter(description = "Ambiente: beta o produccion", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = buildNombreArchivo(ruc, tipoComprobante, serie, numero);
        String amb = resolveAmbiente(ambiente);

        boolean xmlExiste = new File(xmlBasePath + "/" + amb + "/" + ruc + "/" + nombreArchivo + ".xml").exists();
        boolean zipExiste = new File(zipBasePath + "/" + amb + "/" + ruc + "/" + nombreArchivo + ".zip").exists();
        boolean cdrExiste = new File(cdrBasePath + "/" + amb + "/" + ruc + "/R-" + nombreArchivo + ".zip").exists();

        return ResponseEntity.ok(Map.of(
                "ruc", ruc,
                "comprobante", nombreArchivo,
                "ambiente", amb,
                "xmlExiste", xmlExiste,
                "zipExiste", zipExiste,
                "cdrExiste", cdrExiste
        ));
    }

    // ==================== BAJAS ====================

    @Operation(summary = "Descargar XML/ZIP de comunicación de baja",
            description = "Descarga archivos de una comunicación de baja por identificador (ej: RA-20260416-1)")
    @GetMapping("/{ruc}/bajas/{identificador}/{formato}")
    public ResponseEntity<?> descargarBaja(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Identificador de baja", example = "RA-20260416-1") @PathVariable String identificador,
            @Parameter(description = "Formato: xml o zip", example = "xml") @PathVariable String formato,
            @Parameter(description = "Ambiente", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = ruc + "-" + identificador;
        String amb = resolveAmbiente(ambiente);
        String ext = "xml".equals(formato) ? ".xml" : ".zip";
        String subdir = "xml".equals(formato) ? "xml" : "zip";
        String filePath = bajasBasePath + "/" + amb + "/" + ruc + "/" + subdir + "/" + nombreArchivo + ext;

        MediaType mediaType = "xml".equals(formato) ? MediaType.APPLICATION_XML : MediaType.parseMediaType("application/zip");
        return descargarArchivo(filePath, nombreArchivo + ext, mediaType);
    }

    // ==================== RESÚMENES ====================

    @Operation(summary = "Descargar XML/ZIP de resumen diario",
            description = "Descarga archivos de un resumen diario por identificador (ej: RC-20260416-1)")
    @GetMapping("/{ruc}/resumenes/{identificador}/{formato}")
    public ResponseEntity<?> descargarResumen(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Identificador de resumen", example = "RC-20260416-1") @PathVariable String identificador,
            @Parameter(description = "Formato: xml o zip", example = "xml") @PathVariable String formato,
            @Parameter(description = "Ambiente", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = ruc + "-" + identificador;
        String amb = resolveAmbiente(ambiente);
        String ext = "xml".equals(formato) ? ".xml" : ".zip";
        String subdir = "xml".equals(formato) ? "xml" : "zip";
        String filePath = resumenesBasePath + "/" + amb + "/" + ruc + "/" + subdir + "/" + nombreArchivo + ext;

        MediaType mediaType = "xml".equals(formato) ? MediaType.APPLICATION_XML : MediaType.parseMediaType("application/zip");
        return descargarArchivo(filePath, nombreArchivo + ext, mediaType);
    }

    // ==================== GUÍAS ====================

    @Operation(summary = "Descargar XML de guía de remisión")
    @GetMapping("/{ruc}/guias/{tipoGuia}/{serie}/{numero}/xml")
    public ResponseEntity<?> descargarGuiaXml(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Tipo guía: 09 o 31", example = "09") @PathVariable String tipoGuia,
            @Parameter(description = "Serie", example = "T001") @PathVariable String serie,
            @Parameter(description = "Número", example = "1") @PathVariable int numero,
            @Parameter(description = "Ambiente", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = buildNombreArchivo(ruc, tipoGuia, serie, numero);
        String amb = resolveAmbiente(ambiente);
        String filePath = guiasBasePath + "/" + amb + "/" + ruc + "/xml/" + nombreArchivo + ".xml";
        return descargarArchivo(filePath, nombreArchivo + ".xml", MediaType.APPLICATION_XML);
    }

    @Operation(summary = "Descargar ZIP de guía de remisión")
    @GetMapping("/{ruc}/guias/{tipoGuia}/{serie}/{numero}/zip")
    public ResponseEntity<?> descargarGuiaZip(
            @Parameter(description = "RUC del emisor", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Tipo guía: 09 o 31", example = "09") @PathVariable String tipoGuia,
            @Parameter(description = "Serie", example = "T001") @PathVariable String serie,
            @Parameter(description = "Número", example = "1") @PathVariable int numero,
            @Parameter(description = "Ambiente", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = buildNombreArchivo(ruc, tipoGuia, serie, numero);
        String amb = resolveAmbiente(ambiente);
        String filePath = guiasBasePath + "/" + amb + "/" + ruc + "/zip/" + nombreArchivo + ".zip";
        return descargarArchivo(filePath, nombreArchivo + ".zip", MediaType.parseMediaType("application/zip"));
    }

    // ==================== RETENCIONES ====================

    @Operation(summary = "Descargar XML/ZIP de retención")
    @GetMapping("/{ruc}/retenciones/{serie}/{numero}/{formato}")
    public ResponseEntity<?> descargarRetencion(
            @Parameter(description = "RUC", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Serie", example = "R001") @PathVariable String serie,
            @Parameter(description = "Número", example = "1") @PathVariable int numero,
            @Parameter(description = "xml o zip", example = "xml") @PathVariable String formato,
            @Parameter(description = "Ambiente", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = String.format("%s-20-%s-%08d", ruc, serie, numero);
        return descargarGenerico(retencionesBasePath, ruc, nombreArchivo, formato, ambiente);
    }

    // ==================== PERCEPCIONES ====================

    @Operation(summary = "Descargar XML/ZIP de percepción")
    @GetMapping("/{ruc}/percepciones/{serie}/{numero}/{formato}")
    public ResponseEntity<?> descargarPercepcion(
            @Parameter(description = "RUC", example = "20123456789") @PathVariable String ruc,
            @Parameter(description = "Serie", example = "P001") @PathVariable String serie,
            @Parameter(description = "Número", example = "1") @PathVariable int numero,
            @Parameter(description = "xml o zip", example = "xml") @PathVariable String formato,
            @Parameter(description = "Ambiente", example = "beta") @RequestParam(defaultValue = "beta") String ambiente) {

        String nombreArchivo = String.format("%s-40-%s-%08d", ruc, serie, numero);
        return descargarGenerico(percepcionesBasePath, ruc, nombreArchivo, formato, ambiente);
    }

    // ==================== HELPERS ====================

    private ResponseEntity<?> descargarGenerico(String basePath, String ruc, String nombreArchivo, String formato, String ambiente) {
        String amb = resolveAmbiente(ambiente);
        String ext = "xml".equals(formato) ? ".xml" : ".zip";
        String subdir = "xml".equals(formato) ? "xml" : "zip";
        String filePath = basePath + "/" + amb + "/" + ruc + "/" + subdir + "/" + nombreArchivo + ext;
        MediaType mediaType = "xml".equals(formato) ? MediaType.APPLICATION_XML : MediaType.parseMediaType("application/zip");
        return descargarArchivo(filePath, nombreArchivo + ext, mediaType);
    }


    private String buildNombreArchivo(String ruc, String tipoComprobante, String serie, int numero) {
        return String.format("%s-%s-%s-%08d", ruc, tipoComprobante, serie, numero);
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }

    private ResponseEntity<?> descargarArchivo(String filePath, String fileName, MediaType mediaType) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Archivo no encontrado: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(file.length())
                .body(resource);
    }
}

