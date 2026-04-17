package com.facturacion.service;

import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.ComprobanteResponseDTO;
import com.facturacion.dto.EmpresaDTO;
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
 * Tests unitarios para FacturacionService - flujo completo de emisión
 * Mockea SignatureService y SunatSoapService
 */
@ExtendWith(MockitoExtension.class)
class FacturacionServiceTest {

    @Mock
    private SignatureService signatureService;

    @Mock
    private SunatSoapService sunatSoapService;

    @Spy
    private XmlUtilService xmlUtilService;

    private XmlBuilderService xmlBuilderService;

    private FacturacionService facturacionService;

    @BeforeEach
    void setUp() throws Exception {
        xmlBuilderService = new XmlBuilderService();

        facturacionService = new FacturacionService(
                xmlBuilderService, signatureService, xmlUtilService, sunatSoapService);

        ReflectionTestUtils.setField(facturacionService, "xmlBasePath",
                System.getProperty("java.io.tmpdir") + "/test-xml");
        ReflectionTestUtils.setField(facturacionService, "zipBasePath",
                System.getProperty("java.io.tmpdir") + "/test-zip");
        ReflectionTestUtils.setField(facturacionService, "cdrBasePath",
                System.getProperty("java.io.tmpdir") + "/test-cdr");

        // Mock firma: devuelve el documento tal cual (lenient para tests que no lo usan)
        lenient().when(signatureService.sign(any(Document.class), anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== FACTURA GRAVADA ====================

    @Test
    @DisplayName("Factura gravada - flujo completo exitoso")
    void facturaGravadaExitosa() throws Exception {
        mockSunatExitosa();

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.facturaGravada());

        assertTrue(resp.isSuccess());
        assertEquals("01", resp.getTipoComprobante());
        assertEquals("F001-00000001", resp.getSerieCorrelativo());
        assertNotNull(resp.getHashCode());
        assertNotNull(resp.getXmlBase64());
        assertTrue(resp.getTotalGravado().doubleValue() > 0);
        assertEquals(0, resp.getTotalExonerado().doubleValue());
        assertEquals(0, resp.getTotalInafecto().doubleValue());
        assertTrue(resp.getTotalIgv().doubleValue() > 0);
    }

    // ==================== FACTURA EXONERADA ====================

    @Test
    @DisplayName("Factura exonerada - totales correctos en respuesta")
    void facturaExonerada() throws Exception {
        mockSunatExitosa();

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.facturaExonerada());

        assertTrue(resp.isSuccess());
        assertEquals(0, resp.getTotalGravado().doubleValue());
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
        assertEquals(0, resp.getTotalIgv().doubleValue());
    }

    // ==================== FACTURA INAFECTA ====================

    @Test
    @DisplayName("Factura inafecta - totales correctos en respuesta")
    void facturaInafecta() throws Exception {
        mockSunatExitosa();

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.facturaInafecta());

        assertTrue(resp.isSuccess());
        assertEquals(0, resp.getTotalGravado().doubleValue());
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
        assertEquals(0, resp.getTotalIgv().doubleValue());
    }

    // ==================== FACTURA MIXTA ====================

    @Test
    @DisplayName("Factura mixta - todos los totales presentes")
    void facturaMixta() throws Exception {
        mockSunatExitosa();

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.facturaMixta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalGravado().doubleValue() > 0);
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
        assertTrue(resp.getTotalIgv().doubleValue() > 0);
    }

    // ==================== BOLETAS ====================

    @Test
    @DisplayName("Boleta gravada - flujo completo")
    void boletaGravada() throws Exception {
        mockSunatExitosa();

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.boletaGravada());

        assertTrue(resp.isSuccess());
        assertEquals("03", resp.getTipoComprobante());
        assertTrue(resp.getSerieCorrelativo().startsWith("B"));
    }

    @Test
    @DisplayName("Boleta mixta - flujo completo")
    void boletaMixta() throws Exception {
        mockSunatExitosa();

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.boletaMixta());

        assertTrue(resp.isSuccess());
        assertTrue(resp.getTotalGravado().doubleValue() > 0);
        assertTrue(resp.getTotalExonerado().doubleValue() > 0);
        assertTrue(resp.getTotalInafecto().doubleValue() > 0);
    }

    // ==================== ERROR SUNAT ====================

    @Test
    @DisplayName("Error SUNAT - respuesta rechazada")
    void errorSunat() throws Exception {
        SunatSoapService.SunatResponse sunatResp = new SunatSoapService.SunatResponse();
        sunatResp.setSuccess(false);
        sunatResp.setResponseCode("2033");
        sunatResp.setDescription("El XML no cumple con el esquema");
        when(sunatSoapService.sendBill(anyString(), any(), any(EmpresaDTO.class))).thenReturn(sunatResp);

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.facturaGravada());

        assertFalse(resp.isSuccess());
        assertEquals("2033", resp.getSunatResponseCode());
    }

    // ==================== ERROR EXCEPCIÓN ====================

    @Test
    @DisplayName("Excepción durante emisión - respuesta error")
    void excepcion() throws Exception {
        when(signatureService.sign(any(), anyString(), anyString())).thenThrow(new RuntimeException("Error firma"));

        ComprobanteResponseDTO resp = facturacionService.emitirComprobante(TestDataBuilder.facturaGravada());

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("Error"));
    }

    // ==================== GENERAR XML SIN ENVIAR ====================

    @Test
    @DisplayName("Generar XML sin enviar - no llama a SUNAT")
    void generarXmlSinEnviar() throws Exception {
        ComprobanteResponseDTO resp = facturacionService.generarXmlSinEnviar(TestDataBuilder.facturaGravada());

        assertTrue(resp.isSuccess());
        assertNotNull(resp.getXmlBase64());
        verifyNoInteractions(sunatSoapService);
        verifyNoInteractions(signatureService);
    }

    // ==================== HELPERS ====================

    private void mockSunatExitosa() throws Exception {
        SunatSoapService.SunatResponse sunatResp = new SunatSoapService.SunatResponse();
        sunatResp.setSuccess(true);
        sunatResp.setResponseCode("0");
        sunatResp.setDescription("La Factura numero F001-00000001, ha sido aceptada");
        sunatResp.setNotes("");
        when(sunatSoapService.sendBill(anyString(), any(), any(EmpresaDTO.class))).thenReturn(sunatResp);
    }
}
