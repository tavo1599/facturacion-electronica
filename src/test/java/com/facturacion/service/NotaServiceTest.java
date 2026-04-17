package com.facturacion.service;

import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.NotaResponseDTO;
import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests unitarios para NotaService - flujo completo de NC y ND
 */
@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock
    private SignatureService signatureService;

    @Mock
    private SunatSoapService sunatSoapService;

    @Spy
    private XmlUtilService xmlUtilService;

    private NotaXmlBuilderService notaXmlBuilder;
    private NotaService notaService;

    @BeforeEach
    void setUp() throws Exception {
        notaXmlBuilder = new NotaXmlBuilderService();

        notaService = new NotaService(notaXmlBuilder, signatureService, xmlUtilService, sunatSoapService);

        ReflectionTestUtils.setField(notaService, "xmlBasePath",
                System.getProperty("java.io.tmpdir") + "/test-xml");
        ReflectionTestUtils.setField(notaService, "zipBasePath",
                System.getProperty("java.io.tmpdir") + "/test-zip");
        ReflectionTestUtils.setField(notaService, "cdrBasePath",
                System.getProperty("java.io.tmpdir") + "/test-cdr");

        lenient().when(signatureService.sign(any(Document.class), anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== NOTAS DE CRÉDITO ====================

    @Test
    @DisplayName("NC gravada - flujo completo")
    void ncGravada() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoGravada());

        assertTrue(resp.isSuccess());
        assertEquals("07", resp.getTipoNota());
        assertEquals("01", resp.getTipoComprobanteAfectado());
        assertEquals("F001-00000001", resp.getComprobanteAfectado());
        assertTrue(resp.getTotalGravado().doubleValue() > 0);
        assertNotNull(resp.getXmlBase64());
    }

    @Test
    @DisplayName("NC exonerada - flujo completo")
    void ncExonerada() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoExonerada());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
        assertEquals(0, resp.getTotalIgv().doubleValue());
    }

    @Test
    @DisplayName("NC inafecta - flujo completo")
    void ncInafecta() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoInafecta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
    }

    @Test
    @DisplayName("NC mixta - flujo completo")
    void ncMixta() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoMixta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalGravado().doubleValue() > 0);
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
    }

    @Test
    @DisplayName("NC sobre boleta - flujo completo")
    void ncSobreBoleta() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoSobreBoleta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getSerieCorrelativo().startsWith("B"));
    }

    // ==================== NOTAS DE DÉBITO ====================

    @Test
    @DisplayName("ND gravada - flujo completo")
    void ndGravada() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaDebitoGravada());

        assertTrue(resp.isSuccess());
        assertEquals("08", resp.getTipoNota());
        assertEquals("01", resp.getCodigoMotivo());
    }

    @Test
    @DisplayName("ND exonerada - flujo completo")
    void ndExonerada() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaDebitoExonerada());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
    }

    @Test
    @DisplayName("ND inafecta - flujo completo")
    void ndInafecta() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaDebitoInafecta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
    }

    @Test
    @DisplayName("ND mixta - flujo completo")
    void ndMixta() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaDebitoMixta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalGravado().doubleValue() > 0);
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
    }

    @Test
    @DisplayName("ND sobre boleta - flujo completo")
    void ndSobreBoleta() throws Exception {
        mockSunatExitosa();
        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaDebitoSobreBoleta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getSerieCorrelativo().startsWith("B"));
    }

    // ==================== ERRORES ====================

    @Test
    @DisplayName("Error SUNAT rechaza nota")
    void errorSunat() throws Exception {
        SunatSoapService.SunatResponse sunatResp = new SunatSoapService.SunatResponse();
        sunatResp.setSuccess(false);
        sunatResp.setResponseCode("2800");
        sunatResp.setDescription("Documento rechazado");
        when(sunatSoapService.sendBill(anyString(), any(), any(EmpresaDTO.class))).thenReturn(sunatResp);

        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoGravada());
        assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("Excepción durante emisión")
    void excepcion() throws Exception {
        when(signatureService.sign(any(), anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        NotaResponseDTO resp = notaService.emitirNota(TestDataBuilder.notaCreditoGravada());
        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("Error"));
    }

    @Test
    @DisplayName("Generar XML sin enviar")
    void generarSinEnviar() throws Exception {
        NotaResponseDTO resp = notaService.generarXmlSinEnviar(TestDataBuilder.notaCreditoGravada());

        assertTrue(resp.isSuccess());
        assertNotNull(resp.getXmlBase64());
        verifyNoInteractions(sunatSoapService);
    }

    // ==================== HELPERS ====================

    private void mockSunatExitosa() throws Exception {
        SunatSoapService.SunatResponse sunatResp = new SunatSoapService.SunatResponse();
        sunatResp.setSuccess(true);
        sunatResp.setResponseCode("0");
        sunatResp.setDescription("Aceptada");
        sunatResp.setNotes("");
        when(sunatSoapService.sendBill(anyString(), any(), any(EmpresaDTO.class))).thenReturn(sunatResp);
    }
}
