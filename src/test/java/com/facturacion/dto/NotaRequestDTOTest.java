package com.facturacion.dto;

import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para NotaRequestDTO - NC y ND con todos los tipos de afectación
 */
class NotaRequestDTOTest {

    // ==================== NOTAS DE CRÉDITO ====================

    @Nested
    @DisplayName("Nota de Crédito Gravada")
    class NCGravada {

        private final NotaRequestDTO dto = TestDataBuilder.notaCreditoGravada();

        @Test
        void tipoNota() { assertEquals("07", dto.getTipoNota()); }

        @Test
        void serieCorrelativo() { assertEquals("F001-00000001", dto.getSerieCorrelativo()); }

        @Test
        void comprobanteAfectado() { assertEquals("F001-00000001", dto.getComprobanteAfectado()); }

        @Test
        void totalGravado() {
            assertEquals(new BigDecimal("3000.00"), dto.getTotalGravado());
        }

        @Test
        void igv() { assertEquals(new BigDecimal("540.00"), dto.getTotalIgv()); }

        @Test
        void importeTotal() { assertEquals(new BigDecimal("3540.00"), dto.getImporteTotal()); }

        @Test
        void nombreArchivo() {
            assertEquals("20123456789-07-F001-00000001", dto.getNombreArchivo());
        }
    }

    @Nested
    @DisplayName("Nota de Crédito Exonerada")
    class NCExonerada {

        private final NotaRequestDTO dto = TestDataBuilder.notaCreditoExonerada();

        @Test
        void totalExonerado() { assertEquals(new BigDecimal("500.00"), dto.getTotalExonerado()); }

        @Test
        void igvCero() { assertEquals(BigDecimal.ZERO, dto.getTotalIgv()); }

        @Test
        void motivoDevolucion() { assertEquals("06", dto.getCodigoMotivo()); }
    }

    @Nested
    @DisplayName("Nota de Crédito Inafecta")
    class NCInafecta {

        private final NotaRequestDTO dto = TestDataBuilder.notaCreditoInafecta();

        @Test
        void totalInafecto() { assertEquals(new BigDecimal("1000.00"), dto.getTotalInafecto()); }

        @Test
        void igvCero() { assertEquals(BigDecimal.ZERO, dto.getTotalIgv()); }
    }

    @Nested
    @DisplayName("Nota de Crédito Mixta")
    class NCMixta {

        private final NotaRequestDTO dto = TestDataBuilder.notaCreditoMixta();

        @Test
        void todosLosTotales() {
            assertTrue(dto.getTotalGravado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(dto.getTotalExonerado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(dto.getTotalInafecto().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        void igvSoloGravado() {
            BigDecimal expected = dto.getTotalGravado()
                    .multiply(new BigDecimal("0.18")).setScale(2, BigDecimal.ROUND_HALF_UP);
            assertEquals(expected, dto.getTotalIgv());
        }
    }

    @Nested
    @DisplayName("Nota de Crédito sobre Boleta")
    class NCSobreBoleta {

        private final NotaRequestDTO dto = TestDataBuilder.notaCreditoSobreBoleta();

        @Test
        void tipoAfectado() { assertEquals("03", dto.getTipoComprobanteAfectado()); }

        @Test
        void serieBoleta() { assertTrue(dto.getSerie().startsWith("B")); }

        @Test
        void clienteDNI() { assertEquals("1", dto.getClienteTipoDocumento()); }
    }

    // ==================== NOTAS DE DÉBITO ====================

    @Nested
    @DisplayName("Nota de Débito Gravada")
    class NDGravada {

        private final NotaRequestDTO dto = TestDataBuilder.notaDebitoGravada();

        @Test
        void tipoNota() { assertEquals("08", dto.getTipoNota()); }

        @Test
        void totalGravado() { assertEquals(new BigDecimal("150.00"), dto.getTotalGravado()); }

        @Test
        void igv() { assertEquals(new BigDecimal("27.00"), dto.getTotalIgv()); }

        @Test
        void importeTotal() { assertEquals(new BigDecimal("177.00"), dto.getImporteTotal()); }

        @Test
        void motivoMora() { assertEquals("01", dto.getCodigoMotivo()); }
    }

    @Nested
    @DisplayName("Nota de Débito Exonerada")
    class NDExonerada {

        private final NotaRequestDTO dto = TestDataBuilder.notaDebitoExonerada();

        @Test
        void totalExonerado() { assertEquals(new BigDecimal("50.00"), dto.getTotalExonerado()); }

        @Test
        void igvCero() { assertEquals(BigDecimal.ZERO, dto.getTotalIgv()); }

        @Test
        void motivoAumento() { assertEquals("02", dto.getCodigoMotivo()); }
    }

    @Nested
    @DisplayName("Nota de Débito Inafecta")
    class NDInafecta {

        private final NotaRequestDTO dto = TestDataBuilder.notaDebitoInafecta();

        @Test
        void totalInafecto() { assertEquals(new BigDecimal("500.00"), dto.getTotalInafecto()); }

        @Test
        void igvCero() { assertEquals(BigDecimal.ZERO, dto.getTotalIgv()); }
    }

    @Nested
    @DisplayName("Nota de Débito Mixta")
    class NDMixta {

        private final NotaRequestDTO dto = TestDataBuilder.notaDebitoMixta();

        @Test
        void todosLosTotales() {
            assertTrue(dto.getTotalGravado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(dto.getTotalExonerado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(dto.getTotalInafecto().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        void igvSoloGravado() {
            BigDecimal expected = dto.getTotalGravado()
                    .multiply(new BigDecimal("0.18")).setScale(2, BigDecimal.ROUND_HALF_UP);
            assertEquals(expected, dto.getTotalIgv());
        }
    }

    @Nested
    @DisplayName("Nota de Débito sobre Boleta")
    class NDSobreBoleta {

        private final NotaRequestDTO dto = TestDataBuilder.notaDebitoSobreBoleta();

        @Test
        void tipoAfectado() { assertEquals("03", dto.getTipoComprobanteAfectado()); }

        @Test
        void serieBoleta() { assertTrue(dto.getSerie().startsWith("B")); }
    }
}

