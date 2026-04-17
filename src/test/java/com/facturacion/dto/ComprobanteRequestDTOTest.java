package com.facturacion.dto;

import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ComprobanteRequestDTO - totales, serie-correlativo, nomenclatura
 */
class ComprobanteRequestDTOTest {

    // ==================== FACTURA GRAVADA ====================

    @Nested
    @DisplayName("Factura Gravada")
    class FacturaGravada {

        private final ComprobanteRequestDTO dto = TestDataBuilder.facturaGravada();

        @Test
        @DisplayName("Serie-correlativo formato correcto")
        void serieCorrelativo() {
            assertEquals("F001-00000001", dto.getSerieCorrelativo());
        }

        @Test
        @DisplayName("Nombre archivo SUNAT correcto")
        void nombreArchivo() {
            assertEquals("20123456789-01-F001-00000001", dto.getNombreArchivo());
        }

        @Test
        @DisplayName("Total gravado = suma valores venta items gravados")
        void totalGravado() {
            // Item1: 2*1500=3000, Item2: 5*45=225 => 3225
            assertEquals(new BigDecimal("3225.00"), dto.getTotalGravado());
        }

        @Test
        @DisplayName("Total exonerado = 0")
        void totalExonerado() {
            assertEquals(BigDecimal.ZERO, dto.getTotalExonerado());
        }

        @Test
        @DisplayName("Total inafecto = 0")
        void totalInafecto() {
            assertEquals(BigDecimal.ZERO, dto.getTotalInafecto());
        }

        @Test
        @DisplayName("Total IGV = 18% de gravado")
        void totalIgv() {
            // 3225 * 18% = 580.50
            assertEquals(new BigDecimal("580.50"), dto.getTotalIgv());
        }

        @Test
        @DisplayName("Importe total = valor venta + IGV")
        void importeTotal() {
            // 3225 + 580.50 = 3805.50
            assertEquals(new BigDecimal("3805.50"), dto.getImporteTotal());
        }
    }

    // ==================== FACTURA EXONERADA ====================

    @Nested
    @DisplayName("Factura Exonerada")
    class FacturaExonerada {

        private final ComprobanteRequestDTO dto = TestDataBuilder.facturaExonerada();

        @Test
        @DisplayName("Total gravado = 0")
        void totalGravado() {
            assertEquals(BigDecimal.ZERO, dto.getTotalGravado());
        }

        @Test
        @DisplayName("Total exonerado correcto")
        void totalExonerado() {
            // 10*50=500 + 20*8=160 => 660
            assertEquals(new BigDecimal("660.00"), dto.getTotalExonerado());
        }

        @Test
        @DisplayName("Total IGV = 0")
        void totalIgv() {
            assertEquals(BigDecimal.ZERO, dto.getTotalIgv());
        }

        @Test
        @DisplayName("Importe total = valor venta (sin IGV)")
        void importeTotal() {
            assertEquals(new BigDecimal("660.00"), dto.getImporteTotal());
        }
    }

    // ==================== FACTURA INAFECTA ====================

    @Nested
    @DisplayName("Factura Inafecta")
    class FacturaInafecta {

        private final ComprobanteRequestDTO dto = TestDataBuilder.facturaInafecta();

        @Test
        @DisplayName("Total gravado = 0")
        void totalGravado() {
            assertEquals(BigDecimal.ZERO, dto.getTotalGravado());
        }

        @Test
        @DisplayName("Total inafecto correcto")
        void totalInafecto() {
            // 5000 + 2000 = 7000
            assertEquals(new BigDecimal("7000.00"), dto.getTotalInafecto());
        }

        @Test
        @DisplayName("Total IGV = 0")
        void totalIgv() {
            assertEquals(BigDecimal.ZERO, dto.getTotalIgv());
        }

        @Test
        @DisplayName("Importe total = valor venta (sin IGV)")
        void importeTotal() {
            assertEquals(new BigDecimal("7000.00"), dto.getImporteTotal());
        }
    }

    // ==================== FACTURA MIXTA ====================

    @Nested
    @DisplayName("Factura Mixta (Gravado + Exonerado + Inafecto)")
    class FacturaMixta {

        private final ComprobanteRequestDTO dto = TestDataBuilder.facturaMixta();

        @Test
        @DisplayName("Total gravado correcto")
        void totalGravado() {
            // 1*2500 = 2500
            assertEquals(new BigDecimal("2500.00"), dto.getTotalGravado());
        }

        @Test
        @DisplayName("Total exonerado correcto")
        void totalExonerado() {
            // 3*80 = 240
            assertEquals(new BigDecimal("240.00"), dto.getTotalExonerado());
        }

        @Test
        @DisplayName("Total inafecto correcto")
        void totalInafecto() {
            // 1*1200 = 1200
            assertEquals(new BigDecimal("1200.00"), dto.getTotalInafecto());
        }

        @Test
        @DisplayName("IGV solo sobre gravado")
        void totalIgv() {
            // 2500 * 18% = 450
            assertEquals(new BigDecimal("450.00"), dto.getTotalIgv());
        }

        @Test
        @DisplayName("Importe total correcto")
        void importeTotal() {
            // (2500+240+1200) + 450 = 4390
            assertEquals(new BigDecimal("4390.00"), dto.getImporteTotal());
        }

        @Test
        @DisplayName("Valor venta total = suma de todos los items")
        void totalValorVenta() {
            // 2500+240+1200 = 3940
            assertEquals(new BigDecimal("3940.00"), dto.getTotalValorVenta());
        }
    }

    // ==================== BOLETAS ====================

    @Nested
    @DisplayName("Boleta Gravada")
    class BoletaGravada {

        private final ComprobanteRequestDTO dto = TestDataBuilder.boletaGravada();

        @Test
        @DisplayName("Serie empieza con B")
        void serie() {
            assertTrue(dto.getSerie().startsWith("B"));
        }

        @Test
        @DisplayName("Tipo comprobante = 03")
        void tipo() {
            assertEquals("03", dto.getTipoComprobante());
        }

        @Test
        @DisplayName("Totales correctos")
        void totales() {
            // 250 + 45 = 295 gravado
            assertEquals(new BigDecimal("295.00"), dto.getTotalGravado());
            // 295 * 18% = 53.10
            assertEquals(new BigDecimal("53.10"), dto.getTotalIgv());
        }
    }

    @Nested
    @DisplayName("Boleta Exonerada")
    class BoletaExonerada {

        private final ComprobanteRequestDTO dto = TestDataBuilder.boletaExonerada();

        @Test
        @DisplayName("IGV = 0, Exonerado > 0")
        void totales() {
            assertEquals(BigDecimal.ZERO, dto.getTotalIgv());
            assertEquals(new BigDecimal("125.00"), dto.getTotalExonerado());
        }
    }

    @Nested
    @DisplayName("Boleta Inafecta")
    class BoletaInafecta {

        private final ComprobanteRequestDTO dto = TestDataBuilder.boletaInafecta();

        @Test
        @DisplayName("IGV = 0, Inafecto > 0")
        void totales() {
            assertEquals(BigDecimal.ZERO, dto.getTotalIgv());
            assertEquals(new BigDecimal("500.00"), dto.getTotalInafecto());
        }
    }

    @Nested
    @DisplayName("Boleta Mixta")
    class BoletaMixta {

        private final ComprobanteRequestDTO dto = TestDataBuilder.boletaMixta();

        @Test
        @DisplayName("Los tres totales presentes")
        void totales() {
            assertTrue(dto.getTotalGravado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(dto.getTotalExonerado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(dto.getTotalInafecto().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("IGV solo aplica a gravado")
        void igvSoloGravado() {
            BigDecimal igvEsperado = dto.getTotalGravado()
                    .multiply(new BigDecimal("0.18"))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            assertEquals(igvEsperado, dto.getTotalIgv());
        }
    }

    // ==================== FACTURA USD ====================

    @Nested
    @DisplayName("Factura en USD")
    class FacturaUSD {

        private final ComprobanteRequestDTO dto = TestDataBuilder.facturaGravadaUSD();

        @Test
        @DisplayName("Moneda = USD")
        void moneda() {
            assertEquals("USD", dto.getMoneda());
        }

        @Test
        @DisplayName("Totales en USD")
        void totales() {
            assertEquals(new BigDecimal("3000.00"), dto.getTotalGravado());
            assertEquals(new BigDecimal("540.00"), dto.getTotalIgv());
            assertEquals(new BigDecimal("3540.00"), dto.getImporteTotal());
        }
    }
}

