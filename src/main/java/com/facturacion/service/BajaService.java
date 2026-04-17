package com.facturacion.service;

import com.facturacion.dto.BajaRequestDTO;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.ResumenBajaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 * Orquesta el flujo de Comunicación de Baja:
 * XML → Firmar → ZIP → sendSummary → Ticket
 */
@Service
public class BajaService {

    private static final Logger log = LoggerFactory.getLogger(BajaService.class);

    private final BajaXmlBuilderService xmlBuilder;
    private final SignatureService signatureService;
    private final XmlUtilService xmlUtilService;
    private final SunatSoapService sunatSoapService;
    private final TicketStore ticketStore;

    @Value("${storage.bajas-path:./generated-bajas}")
    private String basePath;

    public BajaService(BajaXmlBuilderService xmlBuilder,
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

    public ResumenBajaResponseDTO enviarBaja(BajaRequestDTO request) {
        EmpresaDTO empresa = request.getEmpresa();
        String ruc = empresa.getRuc();
        String nombreBase = request.getNombreArchivo();
        String ambiente = resolveAmbiente(empresa.getAmbiente());

        log.info("========================================");
        log.info("COMUNICACIÓN DE BAJA: {}", nombreBase);
        log.info("========================================");

        try {
            // 1. Generar XML
            log.info("[1/4] Generando XML de baja...");
            Document xmlDoc = xmlBuilder.buildXml(request);

            // 2. Firmar
            log.info("[2/4] Firmando digitalmente...");
            Document signedDoc = signatureService.sign(xmlDoc, ruc, empresa.getAmbiente());

            // 3. Serializar y comprimir
            log.info("[3/4] Serializando y comprimiendo...");
            byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);
            String xmlFileName = nombreBase + ".xml";
            String zipFileName = nombreBase + ".zip";

            String storagePath = basePath + "/" + ambiente + "/" + ruc;
            xmlUtilService.saveToFile(xmlBytes, storagePath + "/xml/" + xmlFileName);

            byte[] zipBytes = xmlUtilService.zipXml(xmlFileName, xmlBytes);
            xmlUtilService.saveToFile(zipBytes, storagePath + "/zip/" + zipFileName);

            // 4. Enviar a SUNAT
            log.info("[4/4] Enviando a SUNAT (sendSummary)...");
            SunatSoapService.SunatTicketResponse ticketResponse =
                    sunatSoapService.sendSummary(zipFileName, zipBytes, empresa);

            log.info("Ticket recibido: {}", ticketResponse.getTicket());

            // Registrar ticket para polling automático
            if (ticketResponse.isSuccess() && ticketResponse.getTicket() != null) {
                ticketStore.addPendingTicket(ticketResponse.getTicket(), "BAJA", nombreBase, empresa);
            }

            return ResumenBajaResponseDTO.builder()
                    .success(ticketResponse.isSuccess())
                    .message(ticketResponse.isSuccess()
                            ? "Comunicación de baja enviada. Consulte el ticket para ver el resultado."
                            : ticketResponse.getMessage())
                    .tipo("BAJA")
                    .identificador(request.getIdentificador())
                    .nombreArchivo(nombreBase)
                    .ticket(ticketResponse.getTicket())
                    .xmlBase64(xmlUtilService.toBase64(xmlBytes))
                    .build();

        } catch (Exception e) {
            log.error("Error al enviar baja: {}", e.getMessage(), e);
            return ResumenBajaResponseDTO.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .tipo("BAJA")
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
            // Guardar CDR si existe
            if (response.getCdrBase64() != null) {
                byte[] cdrBytes = java.util.Base64.getDecoder().decode(response.getCdrBase64());
                String cdrPath = basePath + "/" + ambiente + "/" + empresa.getRuc() + "/cdr";
                xmlUtilService.saveToFile(cdrBytes, cdrPath + "/R-" + ticket + ".zip");
            }

            return ResumenBajaResponseDTO.builder()
                    .success(response.isSuccess())
                    .message(response.isSuccess()
                            ? "Baja procesada correctamente por SUNAT"
                            : "Baja rechazada por SUNAT")
                    .tipo("BAJA")
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
                    .message("Error al consultar estado: " + e.getMessage())
                    .tipo("BAJA")
                    .ticket(ticket)
                    .build();
        }
    }

    private String resolveAmbiente(String ambiente) {
        return "produccion".equalsIgnoreCase(ambiente) ? "produccion" : "beta";
    }
}

