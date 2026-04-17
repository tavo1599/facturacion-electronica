package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.RetencionPercepcionResponseDTO;
import com.facturacion.dto.RetencionRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
public class RetencionService {

    private static final Logger log = LoggerFactory.getLogger(RetencionService.class);

    private final RetencionXmlBuilderService xmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;
    private final SunatConfig config;

    @Value("${storage.retenciones-path:./generated-retenciones}")
    private String basePath;

    public RetencionService(RetencionXmlBuilderService xmlBuilder, SignatureService signatureService,
                            XmlUtilService xmlUtilService, SunatSoapService sunatSoapService, SunatConfig config) {
        this.xmlBuilder = xmlBuilder;
        this.signatureService = signatureService;
        this.xmlUtilService = xmlUtilService;
        this.sunatSoapService = sunatSoapService;
        this.config = config;
    }

    public RetencionPercepcionResponseDTO emitirRetencion(RetencionRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();
        String ambiente = resolveAmbiente(empresa.getAmbiente());

        log.info("========================================");
        log.info("RETENCIÓN: {}", nombreBase);
        log.info("========================================");

        try {
            Document xmlDoc = xmlBuilder.buildXml(request);
            Document signedDoc = signatureService.sign(xmlDoc, ruc, empresa.getAmbiente());

            byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);
            String xmlFileName = nombreBase + ".xml";
            String zipFileName = nombreBase + ".zip";

            String storagePath = basePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(xmlBytes, storagePath + "/xml/" + xmlFileName);

            String hash = xmlUtilService.generateHash(xmlBytes);

            byte[] zipBytes = xmlUtilService.zipXml(xmlFileName, xmlBytes);
            xmlUtilService.saveToFile(zipBytes, storagePath + "/zip/" + zipFileName);

            // Usar endpoint CPE-GEM SFS
            String endpoint = config.getCpeGemEndpoint(empresa.getAmbiente());
            SunatSoapService.SunatResponse sunatResponse =
                    sunatSoapService.sendBillToEndpoint(zipFileName, zipBytes, empresa, endpoint);

            if (sunatResponse.getCdrBase64() != null) {
                byte[] cdrBytes = java.util.Base64.getDecoder().decode(sunatResponse.getCdrBase64());
                xmlUtilService.saveToFile(cdrBytes, storagePath + "/cdr/R-" + zipFileName);
            }

            return RetencionPercepcionResponseDTO.builder()
                    .success(sunatResponse.isSuccess())
                    .message(sunatResponse.isSuccess() ? "Retención emitida correctamente" : "Retención rechazada")
                    .tipo("RETENCION")
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .totalRetenidoPercibido(request.getTotalRetenido())
                    .totalPagadoCobrado(request.getTotalPagado())
                    .sunatResponseCode(sunatResponse.getResponseCode())
                    .sunatDescription(sunatResponse.getDescription())
                    .sunatNote(sunatResponse.getNotes())
                    .hashCode(hash)
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .cdrBase64(sunatResponse.getCdrBase64())
                    .build();

        } catch (Exception e) {
            log.error("Error al emitir retención: {}", e.getMessage(), e);
            return RetencionPercepcionResponseDTO.builder()
                    .success(false).message("Error: " + e.getMessage())
                    .tipo("RETENCION").serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase).build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}

