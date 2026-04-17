package com.facturacion.service;

import com.facturacion.dto.PendingTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

/**
 * Polling automático de tickets pendientes de Bajas y Resúmenes.
 * Consulta periódicamente a SUNAT el estado y guarda CDR al completar.
 */
@Service
@ConditionalOnProperty(name = "ticket.polling.enabled", havingValue = "true", matchIfMissing = true)
public class TicketPollingService {

    private static final Logger log = LoggerFactory.getLogger(TicketPollingService.class);

    private final TicketStore ticketStore;
    private final SunatSoapService sunatSoapService;
    private final XmlUtilService xmlUtilService;

    @Value("${ticket.polling.max-retries:20}")
    private int maxRetries;

    @Value("${storage.bajas-path:./generated-bajas}")
    private String bajasPath;

    @Value("${storage.resumenes-path:./generated-resumenes}")
    private String resumenesPath;

    public TicketPollingService(TicketStore ticketStore, SunatSoapService sunatSoapService,
                                XmlUtilService xmlUtilService) {
        this.ticketStore = ticketStore;
        this.sunatSoapService = sunatSoapService;
        this.xmlUtilService = xmlUtilService;
    }

    @Scheduled(fixedDelayString = "${ticket.polling.interval:30000}")
    public void pollPendingTickets() {
        List<PendingTicket> pending = ticketStore.getPendingTickets();
        if (pending.isEmpty()) return;

        log.info("Polling {} tickets pendientes...", pending.size());

        for (PendingTicket pt : pending) {
            try {
                processTicket(pt);
            } catch (Exception e) {
                log.error("Error procesando ticket {}: {}", pt.getTicket(), e.getMessage());
                ticketStore.updateTicket(pt.getTicket(), "PENDING");
            }
        }
    }

    private void processTicket(PendingTicket pt) throws Exception {
        if (pt.getRetryCount() >= maxRetries) {
            log.warn("Ticket {} excedió máximo de reintentos ({}). Marcando como FAILED.",
                    pt.getTicket(), maxRetries);
            ticketStore.updateTicket(pt.getTicket(), "FAILED");
            return;
        }

        log.info("Consultando ticket: {} (intento {}/{})", pt.getTicket(), pt.getRetryCount() + 1, maxRetries);

        ticketStore.updateTicket(pt.getTicket(), "PROCESSING");

        SunatSoapService.SunatResponse response = sunatSoapService.getStatus(pt.getTicket(), pt.getEmpresa());

        // Código 98 = en proceso, seguir esperando
        if ("98".equals(response.getResponseCode())) {
            log.info("Ticket {} aún en proceso", pt.getTicket());
            ticketStore.updateTicket(pt.getTicket(), "PENDING");
            return;
        }

        // CDR recibido - guardar
        if (response.getCdrBase64() != null) {
            byte[] cdrBytes = Base64.getDecoder().decode(response.getCdrBase64());
            String ambiente = "produccion".equalsIgnoreCase(pt.getAmbiente()) ? "produccion" : "beta";
            String basePath = "BAJA".equals(pt.getTipo()) ? bajasPath : resumenesPath;
            String cdrPath = basePath + "/" + ambiente + "/" + pt.getRuc() + "/cdr";
            xmlUtilService.saveToFile(cdrBytes, cdrPath + "/R-" + pt.getNombreArchivo() + ".zip");
            log.info("CDR guardado para ticket {}", pt.getTicket());
        }

        String finalStatus = response.isSuccess() ? "COMPLETED" : "FAILED";
        ticketStore.updateTicket(pt.getTicket(), finalStatus);

        log.info("Ticket {} → {} | {} - {}",
                pt.getTicket(), finalStatus, response.getResponseCode(), response.getDescription());
    }
}

