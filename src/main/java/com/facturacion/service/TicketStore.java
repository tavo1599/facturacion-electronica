package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.PendingTicket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén de tickets pendientes de Bajas y Resúmenes.
 * Persiste en archivo JSON para sobrevivir reinicios.
 */
@Component
public class TicketStore {

    private static final Logger log = LoggerFactory.getLogger(TicketStore.class);

    @Value("${ticket.polling.storage-path:./pending-tickets.json}")
    private String storagePath;

    private final ConcurrentHashMap<String, PendingTicket> tickets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public TicketStore() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void loadFromDisk() {
        try {
            File file = new File(storagePath);
            if (file.exists()) {
                List<PendingTicket> saved = mapper.readValue(file, new TypeReference<>() {});
                saved.forEach(t -> tickets.put(t.getTicket(), t));
                log.info("Cargados {} tickets pendientes desde {}", saved.size(), storagePath);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar tickets pendientes: {}", e.getMessage());
        }
    }

    public void addPendingTicket(String ticket, String tipo, String nombreArchivo, EmpresaDTO empresa) {
        PendingTicket pt = new PendingTicket();
        pt.setTicket(ticket);
        pt.setTipo(tipo);
        pt.setNombreArchivo(nombreArchivo);
        pt.setRuc(empresa.getRuc());
        pt.setAmbiente(empresa.getAmbiente());
        pt.setEmpresa(empresa);
        pt.setCreatedAt(LocalDateTime.now());
        pt.setRetryCount(0);
        pt.setStatus("PENDING");

        tickets.put(ticket, pt);
        saveToDisk();
        log.info("Ticket registrado: {} ({}) - {}", ticket, tipo, nombreArchivo);
    }

    public void updateTicket(String ticket, String status) {
        PendingTicket pt = tickets.get(ticket);
        if (pt != null) {
            pt.setStatus(status);
            pt.setLastChecked(LocalDateTime.now());
            pt.setRetryCount(pt.getRetryCount() + 1);
            saveToDisk();
        }
    }

    public void removeTicket(String ticket) {
        tickets.remove(ticket);
        saveToDisk();
        log.info("Ticket removido: {}", ticket);
    }

    public List<PendingTicket> getPendingTickets() {
        return new ArrayList<>(tickets.values().stream()
                .filter(t -> "PENDING".equals(t.getStatus()) || "PROCESSING".equals(t.getStatus()))
                .toList());
    }

    public List<PendingTicket> getAllTickets() {
        return new ArrayList<>(tickets.values());
    }

    public PendingTicket getTicket(String ticket) {
        return tickets.get(ticket);
    }

    private void saveToDisk() {
        try {
            mapper.writeValue(new File(storagePath), new ArrayList<>(tickets.values()));
        } catch (Exception e) {
            log.error("Error al guardar tickets: {}", e.getMessage());
        }
    }
}

