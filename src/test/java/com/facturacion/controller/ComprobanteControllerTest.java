package com.facturacion.controller;

import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.ComprobanteResponseDTO;
import com.facturacion.service.FacturacionService;
import com.facturacion.util.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para ComprobanteController - API REST facturas y boletas
 */
@WebMvcTest(ComprobanteController.class)
class ComprobanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacturacionService facturacionService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ==================== FACTURA GRAVADA ====================

    @Test
    @DisplayName("POST /emitir factura gravada - 200 OK")
    void emitirFacturaGravada() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
        mockServiceExitoso(req);

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tipoComprobante").value("01"))
                .andExpect(jsonPath("$.serieCorrelativo").value("F001-00000001"));
    }

    // ==================== FACTURA EXONERADA ====================

    @Test
    @DisplayName("POST /emitir factura exonerada - 200 OK")
    void emitirFacturaExonerada() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaExonerada();
        mockServiceExitoso(req);

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== FACTURA INAFECTA ====================

    @Test
    @DisplayName("POST /emitir factura inafecta - 200 OK")
    void emitirFacturaInafecta() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaInafecta();
        mockServiceExitoso(req);

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== FACTURA MIXTA ====================

    @Test
    @DisplayName("POST /emitir factura mixta - 200 OK")
    void emitirFacturaMixta() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaMixta();
        mockServiceExitoso(req);

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== BOLETA GRAVADA ====================

    @Test
    @DisplayName("POST /emitir boleta gravada - 200 OK")
    void emitirBoletaGravada() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.boletaGravada();
        mockServiceExitoso(req);

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoComprobante").value("03"));
    }

    // ==================== BOLETA MIXTA ====================

    @Test
    @DisplayName("POST /emitir boleta mixta - 200 OK")
    void emitirBoletaMixta() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.boletaMixta();
        mockServiceExitoso(req);

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== GENERAR XML ====================

    @Test
    @DisplayName("POST /generar-xml - 200 OK")
    void generarXml() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();

        when(facturacionService.generarXmlSinEnviar(any())).thenReturn(
                ComprobanteResponseDTO.builder()
                        .success(true).message("XML generado").xmlBase64("base64data")
                        .tipoComprobante("01").serieCorrelativo("F001-00000001")
                        .build());

        mockMvc.perform(post("/api/comprobantes/generar-xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xmlBase64").value("base64data"));
    }

    // ==================== VALIDACIONES ====================

    @Test
    @DisplayName("Factura con serie B → error validación")
    void facturaConSerieB() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
        req.setSerie("B001");

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Boleta con serie F → error validación")
    void boletaConSerieF() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.boletaGravada();
        req.setSerie("F001");

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Request sin items → 400")
    void sinItems() throws Exception {
        ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
        req.setItems(java.util.Collections.emptyList());

        mockMvc.perform(post("/api/comprobantes/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ==================== HEALTH CHECK ====================

    @Test
    @DisplayName("GET /health - 200")
    void health() throws Exception {
        mockMvc.perform(get("/api/comprobantes/health"))
                .andExpect(status().isOk());
    }

    // ==================== HELPERS ====================

    private void mockServiceExitoso(ComprobanteRequestDTO req) {
        when(facturacionService.emitirComprobante(any())).thenReturn(
                ComprobanteResponseDTO.builder()
                        .success(true)
                        .message("Comprobante emitido correctamente")
                        .tipoComprobante(req.getTipoComprobante())
                        .serieCorrelativo(req.getSerieCorrelativo())
                        .totalGravado(req.getTotalGravado())
                        .totalExonerado(req.getTotalExonerado())
                        .totalInafecto(req.getTotalInafecto())
                        .totalIgv(req.getTotalIgv())
                        .importeTotal(req.getImporteTotal())
                        .sunatResponseCode("0")
                        .build());
    }
}

