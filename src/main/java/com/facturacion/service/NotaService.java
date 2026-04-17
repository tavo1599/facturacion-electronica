package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.NotaRequestDTO;
import com.facturacion.dto.NotaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.Base64;

/**
 * Orquesta el flujo completo de emisión de Notas de Crédito y Débito
 */
@Service
public class NotaService {

    private static final Logger log = LoggerFactory.getLogger(NotaService.class);

    private final NotaXmlBuilderService notaXmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;

    @Value("${storage.xml-path:./generated-xml}")
    private String xmlBasePath;

    @Value("${storage.zip-path:./generated-zip}")
    private String zipBasePath;

    @Value("${storage.cdr-path:./received-cdr}")
    private String cdrBasePath;

    public NotaService(NotaXmlBuilderService notaXmlBuilder,
                       SignatureService signatureService,
                       XmlUtilService xmlUtilService,
                       SunatSoapService sunatSoapService) {
        this.notaXmlBuilder = notaXmlBuilder;
        this.signatureService = signatureService;
        this.xmlUtilService = xmlUtilService;
        this.sunatSoapService = sunatSoapService;
    }

    /**
     * Emite una Nota de Crédito o Débito: genera XML, firma, comprime y envía a SUNAT
     */
    public NotaResponseDTO emitirNota(NotaRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();
        String tipoTexto = "07".equals(request.getTipoNota()) ? "NOTA DE CRÉDITO" : "NOTA DE DÉBITO";

        log.info("========================================");
        log.info("INICIANDO EMISIÓN: {} - {} ({})", tipoTexto, nombreBase, empresa.getRazonSocial());
        log.info("Modifica: {} {} | Motivo: {} - {}",
                "01".equals(request.getTipoComprobanteAfectado()) ? "FACTURA" : "BOLETA",
                request.getComprobanteAfectado(),
                request.getCodigoMotivo(),
                request.getDescripcionMotivo());
        log.info("========================================");

        try {
            // PASO 1: Generar XML UBL 2.1
            log.info("[1/5] Generando XML {}...", tipoTexto);
            Document xmlDoc = notaXmlBuilder.buildXml(request);

            // PASO 2: Firmar
            log.info("[2/5] Firmando digitalmente...");
            Document signedDoc = signatureService.sign(xmlDoc, ruc, empresa.getAmbiente());

            // PASO 3: Serializar
            log.info("[3/5] Serializando XML...");
            byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);
            String xmlFileName = nombreBase + ".xml";
            String ambiente = resolveAmbiente(empresa.getAmbiente());
            String xmlPath = xmlBasePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(xmlBytes, xmlPath + "/" + xmlFileName);

            String hash = xmlUtilService.generateHash(xmlBytes);
            log.info("Hash SHA-256: {}", hash);

            // PASO 4: Comprimir
            log.info("[4/5] Comprimiendo en ZIP...");
            byte[] zipBytes = xmlUtilService.zipXml(xmlFileName, xmlBytes);
            String zipFileName = nombreBase + ".zip";
            String zipPath = zipBasePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(zipBytes, zipPath + "/" + zipFileName);

            // PASO 5: Enviar a SUNAT
            log.info("[5/5] Enviando a SUNAT ({})...", empresa.getAmbiente());
            SunatSoapService.SunatResponse sunatResponse =
                    sunatSoapService.sendBill(zipFileName, zipBytes, empresa);

            // Guardar CDR
            if (sunatResponse.getCdrBase64() != null) {
                byte[] cdrBytes = Base64.getDecoder().decode(sunatResponse.getCdrBase64());
                String cdrPath = cdrBasePath + "/" + ambiente + "/" + ruc;
                xmlUtilService.saveToFile(cdrBytes, cdrPath + "/R-" + zipFileName);
            }

            log.info("========================================");
            log.info("RESULTADO: {} - {}",
                    sunatResponse.isSuccess() ? "ACEPTADA" : "RECHAZADA",
                    sunatResponse.getDescription());
            log.info("========================================");

            return NotaResponseDTO.builder()
                    .success(sunatResponse.isSuccess())
                    .message(sunatResponse.isSuccess()
                            ? tipoTexto + " emitida correctamente"
                            : tipoTexto + " rechazada por SUNAT")
                    .tipoNota(request.getTipoNota())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .tipoComprobanteAfectado(request.getTipoComprobanteAfectado())
                    .comprobanteAfectado(request.getComprobanteAfectado())
                    .codigoMotivo(request.getCodigoMotivo())
                    .descripcionMotivo(request.getDescripcionMotivo())
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
            log.error("ERROR al emitir {}: {}", tipoTexto, e.getMessage(), e);

            return NotaResponseDTO.builder()
                    .success(false)
                    .message("Error al procesar " + tipoTexto + ": " + e.getMessage())
                    .tipoNota(request.getTipoNota())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .comprobanteAfectado(request.getComprobanteAfectado())
                    .codigoMotivo(request.getCodigoMotivo())
                    .descripcionMotivo(request.getDescripcionMotivo())
                    .totalGravado(request.getTotalGravado())
                    .totalIgv(request.getTotalIgv())
                    .importeTotal(request.getImporteTotal())
                    .build();
        }
    }

    /**
     * Solo genera el XML sin enviar a SUNAT
     */
    public NotaResponseDTO generarXmlSinEnviar(NotaRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String nombreBase = request.getNombreArchivo();

        try {
            Document xmlDoc = notaXmlBuilder.buildXml(request);
            byte[] xmlBytes = xmlUtilService.documentToBytesIndented(xmlDoc);
            String xmlFileName = nombreBase + ".xml";
            String ambiente = resolveAmbiente(empresa.getAmbiente());
            String xmlPath = xmlBasePath + "/" + ambiente + "/" + empresa.getRuc();
            xmlUtilService.saveToFile(xmlBytes, xmlPath + "/" + xmlFileName);

            return NotaResponseDTO.builder()
                    .success(true)
                    .message("XML generado correctamente (sin firmar ni enviar)")
                    .tipoNota(request.getTipoNota())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .comprobanteAfectado(request.getComprobanteAfectado())
                    .codigoMotivo(request.getCodigoMotivo())
                    .descripcionMotivo(request.getDescripcionMotivo())
                    .totalGravado(request.getTotalGravado())
                    .totalIgv(request.getTotalIgv())
                    .importeTotal(request.getImporteTotal())
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .build();

        } catch (Exception e) {
            log.error("Error al generar XML: {}", e.getMessage(), e);
            return NotaResponseDTO.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}
