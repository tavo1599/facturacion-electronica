package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.PercepcionRequestDTO;
import com.facturacion.dto.RetencionPercepcionResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
public class PercepcionService {

    private static final Logger log = LoggerFactory.getLogger(PercepcionService.class);

    private final PercepcionXmlBuilderService xmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;
    private final SunatConfig config;

    @Value("${storage.percepciones-path:./generated-percepciones}")
    private String basePath;

    public PercepcionService(PercepcionXmlBuilderService xmlBuilder, SignatureService signatureService,
                             XmlUtilService xmlUtilService, SunatSoapService sunatSoapService, SunatConfig config) {
        this.xmlBuilder = xmlBuilder;
        this.signatureService = signatureService;
        this.xmlUtilService = xmlUtilService;
        this.sunatSoapService = sunatSoapService;
        this.config = config;
    }

    public RetencionPercepcionResponseDTO emitirPercepcion(PercepcionRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();
        String ambiente = resolveAmbiente(empresa.getAmbiente());

        log.info("========================================");
        log.info("PERCEPCIÓN: {}", nombreBase);
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

            String endpoint = config.getCpeGemEndpoint(empresa.getAmbiente());
            SunatSoapService.SunatResponse sunatResponse =
                    sunatSoapService.sendBillToEndpoint(zipFileName, zipBytes, empresa, endpoint);

            if (sunatResponse.getCdrBase64() != null) {
                byte[] cdrBytes = java.util.Base64.getDecoder().decode(sunatResponse.getCdrBase64());
                xmlUtilService.saveToFile(cdrBytes, storagePath + "/cdr/R-" + zipFileName);
            }

            return RetencionPercepcionResponseDTO.builder()
                    .success(sunatResponse.isSuccess())
                    .message(sunatResponse.isSuccess() ? "Percepción emitida correctamente" : "Percepción rechazada")
                    .tipo("PERCEPCION")
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .totalRetenidoPercibido(request.getTotalPercibido())
                    .totalPagadoCobrado(request.getTotalCobrado())
                    .sunatResponseCode(sunatResponse.getResponseCode())
                    .sunatDescription(sunatResponse.getDescription())
                    .sunatNote(sunatResponse.getNotes())
                    .hashCode(hash)
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .cdrBase64(sunatResponse.getCdrBase64())
                    .build();

        } catch (Exception e) {
            log.error("Error al emitir percepción: {}", e.getMessage(), e);
            return RetencionPercepcionResponseDTO.builder()
                    .success(false).message("Error: " + e.getMessage())
                    .tipo("PERCEPCION").serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase).build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}

