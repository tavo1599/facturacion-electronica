package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.ComprobanteResponseDTO;
import com.facturacion.dto.EmpresaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 * Servicio principal que orquesta todo el flujo de facturación electrónica:
 * 1. Generar XML UBL 2.1
 * 2. Firmar digitalmente
 * 3. Comprimir en ZIP
 * 4. Enviar a SUNAT
 * 5. Procesar CDR
 *
 * Soporta múltiples empresas: los datos del emisor viajan en cada request.
 */
@Service
public class FacturacionService {

    private static final Logger log = LoggerFactory.getLogger(FacturacionService.class);

    private final XmlBuilderService xmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;

    @Value("${storage.xml-path:./generated-xml}")
    private String xmlBasePath;

    @Value("${storage.zip-path:./generated-zip}")
    private String zipBasePath;

    @Value("${storage.cdr-path:./received-cdr}")
    private String cdrBasePath;

    public FacturacionService(XmlBuilderService xmlBuilder,
                               SignatureService signatureService,
                               XmlUtilService xmlUtilService,
                               SunatSoapService sunatSoapService) {
        this.xmlBuilder = xmlBuilder;
        this.signatureService = signatureService;
        this.xmlUtilService = xmlUtilService;
        this.sunatSoapService = sunatSoapService;
    }

    /**
     * Procesa un comprobante completo: genera XML, firma, comprime y envía a SUNAT
     */
    public ComprobanteResponseDTO emitirComprobante(ComprobanteRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();

        log.info("========================================");
        log.info("INICIANDO EMISIÓN: {}", nombreBase);
        log.info("Empresa: {} ({}) | Tipo: {} | Serie-Correlativo: {}",
            empresa.getRazonSocial(), ruc,
            "01".equals(request.getTipoComprobante()) ? "FACTURA" : "BOLETA",
            request.getSerieCorrelativo());
        log.info("========================================");

        try {
            // === PASO 1: Generar XML UBL 2.1 ===
            log.info("[1/5] Generando XML UBL 2.1...");
            Document xmlDoc = xmlBuilder.buildXml(request);

            // === PASO 2: Firmar digitalmente ===
            log.info("[2/5] Firmando digitalmente...");
            Document signedDoc = signatureService.sign(xmlDoc, ruc, empresa.getAmbiente());

            // === PASO 3: Serializar XML ===
            log.info("[3/5] Serializando XML...");
            byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);
            String xmlFileName = nombreBase + ".xml";

            String ambiente = resolveAmbiente(empresa.getAmbiente());
            String xmlPath = xmlBasePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(xmlBytes, xmlPath + "/" + xmlFileName);
            log.info("XML guardado: {}/{}", xmlPath, xmlFileName);

            String hash = xmlUtilService.generateHash(xmlBytes);
            log.info("Hash SHA-256: {}", hash);

            // === PASO 4: Comprimir en ZIP ===
            log.info("[4/5] Comprimiendo en ZIP...");
            byte[] zipBytes = xmlUtilService.zipXml(xmlFileName, xmlBytes);
            String zipFileName = nombreBase + ".zip";

            String zipPath = zipBasePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(zipBytes, zipPath + "/" + zipFileName);
            log.info("ZIP guardado: {}/{}", zipPath, zipFileName);

            // === PASO 5: Enviar a SUNAT ===
            log.info("[5/5] Enviando a SUNAT ({})...", empresa.getAmbiente());
            SunatSoapService.SunatResponse sunatResponse =
                sunatSoapService.sendBill(zipFileName, zipBytes, empresa);

            // Guardar CDR si existe
            if (sunatResponse.getCdrBase64() != null) {
                byte[] cdrBytes = java.util.Base64.getDecoder().decode(sunatResponse.getCdrBase64());
                String cdrPath = cdrBasePath + "/" + ambiente + "/" + ruc;
                xmlUtilService.saveToFile(cdrBytes, cdrPath + "/R-" + zipFileName);
                log.info("CDR guardado: {}/R-{}", cdrPath, zipFileName);
            }

            log.info("========================================");
            log.info("RESULTADO: {} - {}",
                sunatResponse.isSuccess() ? "ACEPTADA" : "RECHAZADA",
                sunatResponse.getDescription());
            log.info("========================================");

            return ComprobanteResponseDTO.builder()
                    .success(sunatResponse.isSuccess())
                    .message(sunatResponse.isSuccess()
                        ? "Comprobante emitido correctamente"
                        : "Comprobante rechazado por SUNAT")
                    .tipoComprobante(request.getTipoComprobante())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .totalGravado(request.getTotalGravado())
                    .totalExonerado(request.getTotalExonerado())
                    .totalInafecto(request.getTotalInafecto())
                    .totalIgv(request.getTotalIgv())
                    .importeTotal(request.getImporteTotal())
                    .sunatResponseCode(sunatResponse.getResponseCode())
                    .sunatDescription(sunatResponse.getDescription())
                    .sunatNote(sunatResponse.getNotes())
                    .hashCode(hash)
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .cdrBase64(sunatResponse.getCdrBase64())
                    .build();

        } catch (Exception e) {
            log.error("ERROR al emitir comprobante {}: {}", nombreBase, e.getMessage(), e);

            return ComprobanteResponseDTO.builder()
                    .success(false)
                    .message("Error al procesar el comprobante: " + e.getMessage())
                    .tipoComprobante(request.getTipoComprobante())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .totalGravado(request.getTotalGravado())
                    .totalExonerado(request.getTotalExonerado())
                    .totalInafecto(request.getTotalInafecto())
                    .totalIgv(request.getTotalIgv())
                    .importeTotal(request.getImporteTotal())
                    .build();
        }
    }

    /**
     * Solo genera el XML sin enviarlo a SUNAT (útil para pruebas)
     */
    public ComprobanteResponseDTO generarXmlSinEnviar(ComprobanteRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String nombreBase = request.getNombreArchivo();
        log.info("Generando XML sin enviar: {}", nombreBase);

        try {
            Document xmlDoc = xmlBuilder.buildXml(request);
            byte[] xmlBytes = xmlUtilService.documentToBytesIndented(xmlDoc);
            String xmlFileName = nombreBase + ".xml";

            String ambiente = resolveAmbiente(empresa.getAmbiente());
            String xmlPath = xmlBasePath + "/" + ambiente + "/" + empresa.getRuc();
            xmlUtilService.saveToFile(xmlBytes, xmlPath + "/" + xmlFileName);

            return ComprobanteResponseDTO.builder()
                    .success(true)
                    .message("XML generado correctamente (sin firmar ni enviar)")
                    .tipoComprobante(request.getTipoComprobante())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .totalGravado(request.getTotalGravado())
                    .totalExonerado(request.getTotalExonerado())
                    .totalInafecto(request.getTotalInafecto())
                    .totalIgv(request.getTotalIgv())
                    .importeTotal(request.getImporteTotal())
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .build();

        } catch (Exception e) {
            log.error("Error al generar XML: {}", e.getMessage(), e);
            return ComprobanteResponseDTO.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}
