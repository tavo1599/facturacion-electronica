package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.ResumenBajaResponseDTO;
import com.facturacion.dto.ResumenDiarioRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 * Orquesta el flujo de Resumen Diario de Boletas:
 * XML → Firmar → ZIP → sendSummary → Ticket
 */
@Service
public class ResumenDiarioService {

    private static final Logger log = LoggerFactory.getLogger(ResumenDiarioService.class);

    private final ResumenDiarioXmlBuilderService xmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;
    private final TicketStore ticketStore;

    @Value("${storage.resumenes-path:./generated-resumenes}")
    private String basePath;

    public ResumenDiarioService(ResumenDiarioXmlBuilderService xmlBuilder,
                                SignatureService signatureService,
                                XmlUtilService xmlUtilService,
                                SunatSoapService sunatSoapService,
                                TicketStore ticketStore) {
        this.xmlBuilder = xmlBuilder;
        this.signatureService = signatureService;
        this.xmlUtilService = xmlUtilService;
        this.sunatSoapService = sunatSoapService;
        this.ticketStore = ticketStore;
    }

    public ResumenBajaResponseDTO enviarResumen(ResumenDiarioRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();
        String ambiente = resolveAmbiente(empresa.getAmbiente());

        log.info("========================================");
        log.info("RESUMEN DIARIO: {}", nombreBase);
        log.info("========================================");

        try {
            log.info("[1/4] Generando XML de resumen...");
            Document xmlDoc = xmlBuilder.buildXml(request);

            log.info("[2/4] Firmando digitalmente...");
            Document signedDoc = signatureService.sign(xmlDoc, ruc, empresa.getAmbiente());

            log.info("[3/4] Serializando y comprimiendo...");
            byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);
            String xmlFileName = nombreBase + ".xml";
            String zipFileName = nombreBase + ".zip";

            String storagePath = basePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(xmlBytes, storagePath + "/xml/" + xmlFileName);

            byte[] zipBytes = xmlUtilService.zipXml(xmlFileName, xmlBytes);
            xmlUtilService.saveToFile(zipBytes, storagePath + "/zip/" + zipFileName);

            log.info("[4/4] Enviando a SUNAT (sendSummary)...");
            SunatSoapService.SunatTicketResponse ticketResponse =
                    sunatSoapService.sendSummary(zipFileName, zipBytes, empresa);

            log.info("Ticket recibido: {}", ticketResponse.getTicket());

            if (ticketResponse.isSuccess() && ticketResponse.getTicket() != null) {
                ticketStore.addPendingTicket(ticketResponse.getTicket(), "RESUMEN", nombreBase, empresa);
            }

            return ResumenBajaResponseDTO.builder()
                    .success(ticketResponse.isSuccess())
                    .message(ticketResponse.isSuccess()
                            ? "Resumen diario enviado. Consulte el ticket para ver el resultado."
                            : ticketResponse.getMessage())
                    .tipo("RESUMEN")
                    .identificador(request.getIdentificador())
                    .nombreArchivo(nombreBase)
                    .ticket(ticketResponse.getTicket())
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .build();

        } catch (Exception e) {
            log.error("Error al enviar resumen: {}", e.getMessage(), e);
            return ResumenBajaResponseDTO.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .tipo("RESUMEN")
                    .identificador(request.getIdentificador())
                    .nombreArchivo(nombreBase)
                    .build();
        }
    }

    public ResumenBajaResponseDTO consultarEstado(String ticket, EmpresaDTO empresa) {
        try {
            log.info("Consultando estado de ticket: {}", ticket);
            SunatSoapService.SunatResponse response = sunatSoapService.getStatus(ticket, empresa);

            String ambiente = resolveAmbiente(empresa.getAmbiente());
            if (response.getCdrBase64() != null) {
                byte[] cdrBytes = java.util.Base64.getDecoder().decode(response.getCdrBase64());
                String cdrPath = basePath + "/" + ambiente + "/" + empresa.getRuc() + "/cdr";
                xmlUtilService.saveToFile(cdrBytes, cdrPath + "/R-" + ticket + ".zip");
            }

            return ResumenBajaResponseDTO.builder()
                    .success(response.isSuccess())
                    .message(response.isSuccess()
                            ? "Resumen procesado correctamente por SUNAT"
                            : "Resumen rechazado por SUNAT")
                    .tipo("RESUMEN")
                    .ticket(ticket)
                    .sunatResponseCode(response.getResponseCode())
                    .sunatDescription(response.getDescription())
                    .sunatNote(response.getNotes())
                    .cdrBase64(response.getCdrBase64())
                    .build();

        } catch (Exception e) {
            log.error("Error al consultar estado: {}", e.getMessage(), e);
            return ResumenBajaResponseDTO.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .tipo("RESUMEN")
                    .ticket(ticket)
                    .build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}

