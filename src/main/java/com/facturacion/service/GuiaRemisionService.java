package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.GuiaRemisionRequestDTO;
import com.facturacion.dto.GuiaRemisionResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 * Orquesta el flujo de Guía de Remisión:
 * XML → Firmar → ZIP → sendBill → CDR
 */
@Service
public class GuiaRemisionService {

    private static final Logger log = LoggerFactory.getLogger(GuiaRemisionService.class);

    private final GuiaRemisionXmlBuilderService xmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;
    private final SunatRestService sunatRestService;
    private final SunatConfig config;

    @Value("${storage.guias-path:./generated-guias}")
    private String basePath;

    public GuiaRemisionService(GuiaRemisionXmlBuilderService xmlBuilder,
                               SignatureService signatureService,
                               XmlUtilService xmlUtilService,
                               SunatSoapService sunatSoapService,
                               SunatRestService sunatRestService,
                               SunatConfig config) {
        this.xmlBuilder = xmlBuilder;
        this.signatureService = signatureService;
        this.xmlUtilService = xmlUtilService;
        this.sunatSoapService = sunatSoapService;
        this.sunatRestService = sunatRestService;
        this.config = config;
    }

    public GuiaRemisionResponseDTO emitirGuia(GuiaRemisionRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();
        String ambiente = resolveAmbiente(empresa.getAmbiente());

        log.info("========================================");
        log.info("GUÍA DE REMISIÓN: {}", nombreBase);
        log.info("========================================");

        try {
            log.info("[1/5] Generando XML de guía...");
            Document xmlDoc = xmlBuilder.buildXml(request);

            log.info("[2/5] Firmando digitalmente...");
            Document signedDoc = signatureService.sign(xmlDoc, ruc, empresa.getAmbiente());

            log.info("[3/5] Serializando XML...");
            byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);
            String xmlFileName = nombreBase + ".xml";
            String zipFileName = nombreBase + ".zip";

            String storagePath = basePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(xmlBytes, storagePath + "/xml/" + xmlFileName);

            String hash = xmlUtilService.generateHash(xmlBytes);

            log.info("[4/5] Comprimiendo en ZIP...");
            byte[] zipBytes = xmlUtilService.zipXml(xmlFileName, xmlBytes);
            xmlUtilService.saveToFile(zipBytes, storagePath + "/zip/" + zipFileName);

            log.info("[5/5] Enviando a SUNAT ({})...",
                    useRest(empresa) ? "REST" : "SOAP");

            String responseCode = null;
            String description = null;
            String notes = null;
            String cdrBase64 = null;
            boolean success;

            if (useRest(empresa)) {
                // Envío REST (producción o cuando hay credenciales OAuth2)
                SunatRestService.RestResponse restResponse =
                        sunatRestService.sendGuia(nombreBase, zipBytes, empresa);
                success = restResponse.isSuccess();
                responseCode = restResponse.getResponseCode();
                description = restResponse.getDescription() != null
                        ? restResponse.getDescription() : restResponse.getMessage();
                cdrBase64 = restResponse.getCdrBase64();
            } else {
                // Envío SOAP (beta sin credenciales REST) - endpoint específico guías
                String guiaEndpoint = config.getGuiaEndpoint(empresa.getAmbiente());
                SunatSoapService.SunatResponse sunatResponse =
                        sunatSoapService.sendBillToEndpoint(zipFileName, zipBytes, empresa, guiaEndpoint);
                success = sunatResponse.isSuccess();
                responseCode = sunatResponse.getResponseCode();
                description = sunatResponse.getDescription();
                notes = sunatResponse.getNotes();
                cdrBase64 = sunatResponse.getCdrBase64();
            }

            if (cdrBase64 != null) {
                byte[] cdrBytes = java.util.Base64.getDecoder().decode(cdrBase64);
                xmlUtilService.saveToFile(cdrBytes, storagePath + "/cdr/R-" + zipFileName);
            }

            log.info("RESULTADO: {} - {}",
                    success ? "ACEPTADA" : "RECHAZADA", description);

            return GuiaRemisionResponseDTO.builder()
                    .success(success)
                    .message(success
                            ? "Guía de remisión emitida correctamente"
                            : "Guía rechazada por SUNAT")
                    .tipoGuia(request.getTipoGuia())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .sunatResponseCode(responseCode)
                    .sunatDescription(description)
                    .sunatNote(notes)
                    .hashCode(hash)
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .cdrBase64(cdrBase64)
                    .build();

        } catch (Exception e) {
            log.error("Error al emitir guía: {}", e.getMessage(), e);
            return GuiaRemisionResponseDTO.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .tipoGuia(request.getTipoGuia())
                    .serieCorrelativo(request.getSerieCorrelativo())
                    .nombreArchivo(nombreBase)
                    .build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }

    /**
     * Determina si se debe usar REST.
     * Usa REST si hay credenciales OAuth2 en el request O en el yml.
     */
    private boolean useRest(EmpresaDTO empresa) {
        // Credenciales en el request
        if (empresa.getClientId() != null && !empresa.getClientId().isBlank()
                && empresa.getClientSecret() != null && !empresa.getClientSecret().isBlank()) {
            return true;
        }
        // Credenciales en el yml
        SunatConfig.GreAmbiente greCreds = config.getGreCredentials(empresa.getAmbiente());
        return greCreds.getClientId() != null && !greCreds.getClientId().isBlank()
                && greCreds.getClientSecret() != null && !greCreds.getClientSecret().isBlank();
    }
}

