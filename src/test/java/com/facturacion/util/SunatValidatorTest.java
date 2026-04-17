package com.facturacion.util;

import com.facturacion.dto.ComprobanteLineaDTO;
import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.NotaRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para SunatValidator - validaciones según normativa SUNAT
 */
class SunatValidatorTest {

    // ==================== COMPROBANTE ====================

    @Nested
    @DisplayName("Validar Comprobante")
    class ValidarComprobante {

        @Test
        @DisplayName("Factura gravada válida - sin errores")
        void facturaGravadaValida() {
            List<String> errores = SunatValidator.validarComprobante(TestDataBuilder.facturaGravada());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("Factura exonerada válida")
        void facturaExoneradaValida() {
            List<String> errores = SunatValidator.validarComprobante(TestDataBuilder.facturaExonerada());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("Factura inafecta válida")
        void facturaInafectaValida() {
            List<String> errores = SunatValidator.validarComprobante(TestDataBuilder.facturaInafecta());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("Factura mixta válida")
        void facturaMixtaValida() {
            List<String> errores = SunatValidator.validarComprobante(TestDataBuilder.facturaMixta());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("Boleta gravada válida")
        void boletaGravadaValida() {
            List<String> errores = SunatValidator.validarComprobante(TestDataBuilder.boletaGravada());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("Boleta mixta válida")
        void boletaMixtaValida() {
            List<String> errores = SunatValidator.validarComprobante(TestDataBuilder.boletaMixta());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("Factura con serie B → error")
        void facturaSerieB() {
            ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
            req.setSerie("B001");
            List<String> errores = SunatValidator.validarComprobante(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("serie")), "Debe detectar serie incorrecta");
        }

        @Test
        @DisplayName("Boleta con serie F → error")
        void boletaSerieF() {
            ComprobanteRequestDTO req = TestDataBuilder.boletaGravada();
            req.setSerie("F001");
            List<String> errores = SunatValidator.validarComprobante(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("serie")));
        }

        @Test
        @DisplayName("Factura con DNI → error (debe ser RUC)")
        void facturaConDni() {
            ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
            req.setClienteTipoDocumento("1");
            req.setClienteNumeroDocumento("45678912");
            List<String> errores = SunatValidator.validarComprobante(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("RUC")));
        }

        @Test
        @DisplayName("Factura con RUC de 10 dígitos → error")
        void facturaRucCorto() {
            ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
            req.setClienteNumeroDocumento("2051234567");
            List<String> errores = SunatValidator.validarComprobante(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("11 dígitos")));
        }

        @Test
        @DisplayName("Factura con RUC prefijo inválido → error")
        void facturaRucPrefijoInvalido() {
            ComprobanteRequestDTO req = TestDataBuilder.facturaGravada();
            req.setClienteNumeroDocumento("30512345678");
            List<String> errores = SunatValidator.validarComprobante(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("empezar con 10, 15, 17 o 20")));
        }

        @Test
        @DisplayName("DNI de 7 dígitos → error")
        void dniCorto() {
            ComprobanteRequestDTO req = TestDataBuilder.boletaGravada();
            req.setClienteNumeroDocumento("4567891");
            List<String> errores = SunatValidator.validarComprobante(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("8 dígitos")));
        }
    }

    // ==================== VALIDACIÓN DE ITEMS ====================

    @Nested
    @DisplayName("Validar Items")
    class ValidarItems {

        @Test
        @DisplayName("Unidad de medida NIU válida")
        void unidadNIU() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setUnidadMedida("NIU");
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.isEmpty());
        }

        @Test
        @DisplayName("Unidad de medida ZZ (servicio) válida")
        void unidadZZ() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setUnidadMedida("ZZ");
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.isEmpty());
        }

        @Test
        @DisplayName("Unidad de medida BX (caja) válida")
        void unidadBX() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setUnidadMedida("BX");
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.isEmpty());
        }

        @Test
        @DisplayName("Unidad de medida inválida → error")
        void unidadInvalida() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setUnidadMedida("XYZ");
            SunatValidator.validarItem(item, errores);
            assertFalse(errores.isEmpty());
            assertTrue(errores.get(0).contains("Unidad de medida"));
        }

        @Test
        @DisplayName("Tipo afectación IGV 10 (gravado) válido")
        void afectacionGravado() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.isEmpty());
        }

        @Test
        @DisplayName("Tipo afectación IGV 99 → error")
        void afectacionInvalida() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setTipoAfectacionIgv("99");
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.stream().anyMatch(e -> e.contains("Catálogo 07")));
        }

        @Test
        @DisplayName("Exonerado con porcentaje IGV 18% → error")
        void exoneradoConIgv() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemExonerado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setPorcentajeIgv(new BigDecimal("18.00"));
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.stream().anyMatch(e -> e.contains("0%")));
        }

        @Test
        @DisplayName("Gravado con código tributo 9997 → error coherencia")
        void gravadoConTributoExonerado() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setCodigoTributo("9997");
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.stream().anyMatch(e -> e.contains("no corresponde")));
        }

        @Test
        @DisplayName("Exonerado: precio unitario ≠ valor unitario → error")
        void exoneradoPrecioIncorrecto() {
            List<String> errores = new ArrayList<>();
            ComprobanteLineaDTO item = TestDataBuilder.itemExonerado(1, "Test",
                    new BigDecimal("1"), new BigDecimal("100"));
            item.setPrecioUnitario(new BigDecimal("118.00")); // Incorrecto para exonerado
            SunatValidator.validarItem(item, errores);
            assertTrue(errores.stream().anyMatch(e -> e.contains("igual a valorUnitario")));
        }
    }

    // ==================== VALIDACIÓN DE NOTAS ====================

    @Nested
    @DisplayName("Validar Notas")
    class ValidarNotas {

        @Test
        @DisplayName("NC gravada válida")
        void ncGravadaValida() {
            List<String> errores = SunatValidator.validarNota(TestDataBuilder.notaCreditoGravada());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("NC mixta válida")
        void ncMixtaValida() {
            List<String> errores = SunatValidator.validarNota(TestDataBuilder.notaCreditoMixta());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("ND gravada válida")
        void ndGravadaValida() {
            List<String> errores = SunatValidator.validarNota(TestDataBuilder.notaDebitoGravada());
            assertTrue(errores.isEmpty(), "Errores: " + errores);
        }

        @Test
        @DisplayName("NC con serie B modifica factura → error")
        void ncSerieInconsistente() {
            NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();
            req.setSerie("B001");
            List<String> errores = SunatValidator.validarNota(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("serie")));
        }

        @Test
        @DisplayName("NC motivo inválido → error")
        void ncMotivoInvalido() {
            NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();
            req.setCodigoMotivo("99");
            List<String> errores = SunatValidator.validarNota(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("motivo")));
        }

        @Test
        @DisplayName("ND motivo 06 (no válido para ND) → error")
        void ndMotivoInvalido() {
            NotaRequestDTO req = TestDataBuilder.notaDebitoGravada();
            req.setCodigoMotivo("06");
            List<String> errores = SunatValidator.validarNota(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("motivo")));
        }

        @Test
        @DisplayName("Formato comprobante afectado inválido → error")
        void comprobanteAfectadoInvalido() {
            NotaRequestDTO req = TestDataBuilder.notaCreditoGravada();
            req.setComprobanteAfectado("INVALIDO");
            List<String> errores = SunatValidator.validarNota(req);
            assertTrue(errores.stream().anyMatch(e -> e.contains("Formato")));
        }
    }

    // ==================== HELPERS UblConstants ====================

    @Nested
    @DisplayName("UblConstants helpers")
    class Helpers {

        @Test
        @DisplayName("getTaxCategoryId para gravado = S")
        void taxCatGravado() {
            assertEquals("S", UblConstants.getTaxCategoryId("10"));
            assertEquals("S", UblConstants.getTaxCategoryId("17"));
        }

        @Test
        @DisplayName("getTaxCategoryId para exonerado = E")
        void taxCatExonerado() {
            assertEquals("E", UblConstants.getTaxCategoryId("20"));
            assertEquals("E", UblConstants.getTaxCategoryId("21"));
        }

        @Test
        @DisplayName("getTaxCategoryId para inafecto = O")
        void taxCatInafecto() {
            assertEquals("O", UblConstants.getTaxCategoryId("30"));
            assertEquals("O", UblConstants.getTaxCategoryId("36"));
        }

        @Test
        @DisplayName("getTaxCategoryId para exportación = Z")
        void taxCatExportacion() {
            assertEquals("Z", UblConstants.getTaxCategoryId("40"));
        }

        @Test
        @DisplayName("esOperacionGratuita")
        void operacionGratuita() {
            assertFalse(UblConstants.esOperacionGratuita("10")); // Gravado oneroso
            assertTrue(UblConstants.esOperacionGratuita("11"));  // Retiro premio
            assertTrue(UblConstants.esOperacionGratuita("15"));  // Bonificaciones
            assertFalse(UblConstants.esOperacionGratuita("20")); // Exonerado oneroso
            assertTrue(UblConstants.esOperacionGratuita("21"));  // Exonerado gratuito
            assertFalse(UblConstants.esOperacionGratuita("30")); // Inafecto oneroso
            assertTrue(UblConstants.esOperacionGratuita("31"));  // Inafecto retiro
        }

        @Test
        @DisplayName("Unidades de medida contiene las principales")
        void unidadesValidas() {
            assertTrue(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("NIU"));
            assertTrue(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("ZZ"));
            assertTrue(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("KGM"));
            assertTrue(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("BX"));
            assertTrue(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("LTR"));
            assertTrue(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("HUR"));
            assertFalse(UblConstants.UNIDADES_MEDIDA_VALIDAS.contains("XYZ"));
        }

        @Test
        @DisplayName("Tipos documento válidos")
        void tiposDocumento() {
            assertTrue(UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains("6")); // RUC
            assertTrue(UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains("1")); // DNI
            assertTrue(UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains("0")); // Sin doc
            assertTrue(UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains("4")); // CE
            assertTrue(UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains("7")); // Pasaporte
            assertFalse(UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains("9")); // No existe
        }
    }
}

