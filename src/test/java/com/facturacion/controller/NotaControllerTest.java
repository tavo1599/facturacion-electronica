package com.facturacion.controller;

import com.facturacion.dto.NotaRequestDTO;
import com.facturacion.dto.NotaResponseDTO;
import com.facturacion.service.NotaService;
import com.facturacion.util.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para NotaController - API REST notas de crédito y débito
 */
@WebMvcTest(NotaController.class)
class NotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotaService notaService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ==================== NC ====================

    @Test
    @DisplayName("POST /emitir NC gravada - 200")
    void emitirNCGravada() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tipoNota").value("07"));
    }

    @Test
    @DisplayName("POST /emitir NC exonerada - 200")
    void emitirNCExonerada() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoExonerada();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /emitir NC mixta - 200")
    void emitirNCMixta() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoMixta();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /emitir NC sobre boleta - 200")
    void emitirNCSobreBoleta() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoSobreBoleta();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ==================== ND ====================

    @Test
    @DisplayName("POST /emitir ND gravada - 200")
    void emitirNDGravada() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaDebitoGravada();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoNota").value("08"));
    }

    @Test
    @DisplayName("POST /emitir ND mixta - 200")
    void emitirNDMixta() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaDebitoMixta();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /emitir ND sobre boleta - 200")
    void emitirNDSobreBoleta() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaDebitoSobreBoleta();
        mockNotaExitosa(req);

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ==================== GENERAR XML ====================

    @Test
    @DisplayName("POST /generar-xml NC - 200")
    void generarXmlNC() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();

        when(notaService.generarXmlSinEnviar(any())).thenReturn(
                NotaResponseDTO.builder()
                        .success(true).tipoNota("07")
                        .serieCorrelativo("F001-00000001")
                        .xmlBase64("b64").build());

        mockMvc.perform(post("/api/notas/generar-xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xmlBase64").value("b64"));
    }

    // ==================== VALIDACIONES ====================

    @Test
    @DisplayName("NC con motivo inválido → error")
    void motivoInvalido() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();
        req.setCodigoMotivo("99");

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("NC modifica factura pero serie B → error")
    void serieInconsistente() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();
        req.setSerie("B001");

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ND con motivo inválido para catálogo 10 → error")
    void ndMotivoInvalido() throws Exception {
        NotaRequestDTO req = TestDataBuilder.notaDebitoGravada();
        req.setCodigoMotivo("06"); // no válido para ND

        mockMvc.perform(post("/api/notas/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ==================== HELPERS ====================

    private void mockNotaExitosa(NotaRequestDTO req) {
        when(notaService.emitirNota(any())).thenReturn(
                NotaResponseDTO.builder()
                        .success(true)
                        .message("Nota emitida correctamente")
                        .tipoNota(req.getTipoNota())
                        .serieCorrelativo(req.getSerieCorrelativo())
                        .comprobanteAfectado(req.getComprobanteAfectado())
                        .codigoMotivo(req.getCodigoMotivo())
                        .totalGravado(req.getTotalGravado())
                        .totalExonerado(req.getTotalExonerado())
                        .totalInafecto(req.getTotalInafecto())
                        .totalIgv(req.getTotalIgv())
                        .importeTotal(req.getImporteTotal())
                        .sunatResponseCode("0")
                        .build());
    }
}

