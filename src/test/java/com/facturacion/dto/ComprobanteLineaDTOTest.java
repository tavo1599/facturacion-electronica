package com.facturacion.dto;

import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ComprobanteLineaDTO - cálculos de valores, IGV según tipo afectación
 */
class ComprobanteLineaDTOTest {

    @Nested
    @DisplayName("Item Gravado (IGV 18%)")
    class ItemGravado {

        @Test
        @DisplayName("Calcula valor venta = cantidad * valorUnitario")
        void valorVenta() {
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Producto", 
                    new BigDecimal("3"), new BigDecimal("100.00"));
            assertEquals(new BigDecimal("300.00"), item.getValorVenta());
        }

        @Test
        @DisplayName("Calcula IGV 18% sobre valor venta")
        void montoIgv() {
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Producto",
                    new BigDecimal("2"), new BigDecimal("500.00"));
            // 1000 * 18% = 180
            assertEquals(new BigDecimal("180.00"), item.getMontoIgv());
        }

        @Test
        @DisplayName("Calcula total = valorVenta + IGV")
        void montoTotal() {
            ComprobanteLineaDTO item = TestDataBuilder.itemGravado(1, "Producto",
                    new BigDecimal("1"), new BigDecimal("1000.00"));
            // 1000 + 180 = 1180
            assertEquals(new BigDecimal("1180.00"), item.getMontoTotal());
        }
    }

    @Nested
    @DisplayName("Item Exonerado (IGV 0%)")
    class ItemExonerado {

        @Test
        @DisplayName("Valor venta correcto")
        void valorVenta() {
            ComprobanteLineaDTO item = TestDataBuilder.itemExonerado(1, "Libro",
                    new BigDecimal("5"), new BigDecimal("50.00"));
            assertEquals(new BigDecimal("250.00"), item.getValorVenta());
        }

        @Test
        @DisplayName("IGV es cero para exonerado")
        void igvCero() {
            ComprobanteLineaDTO item = TestDataBuilder.itemExonerado(1, "Libro",
                    new BigDecimal("5"), new BigDecimal("50.00"));
            assertEquals(BigDecimal.ZERO, item.getMontoIgv());
        }

        @Test
        @DisplayName("Total igual a valor venta (sin IGV)")
        void totalIgualValorVenta() {
            ComprobanteLineaDTO item = TestDataBuilder.itemExonerado(1, "Libro",
                    new BigDecimal("5"), new BigDecimal("50.00"));
            assertEquals(item.getValorVenta(), item.getMontoTotal());
        }
    }

    @Nested
    @DisplayName("Item Inafecto (IGV 0%)")
    class ItemInafecto {

        @Test
        @DisplayName("Valor venta correcto")
        void valorVenta() {
            ComprobanteLineaDTO item = TestDataBuilder.itemInafecto(1, "Servicio",
                    new BigDecimal("1"), new BigDecimal("5000.00"));
            assertEquals(new BigDecimal("5000.00"), item.getValorVenta());
        }

        @Test
        @DisplayName("IGV es cero para inafecto")
        void igvCero() {
            ComprobanteLineaDTO item = TestDataBuilder.itemInafecto(1, "Servicio",
                    new BigDecimal("1"), new BigDecimal("5000.00"));
            assertEquals(BigDecimal.ZERO, item.getMontoIgv());
        }

        @Test
        @DisplayName("Total igual a valor venta (sin IGV)")
        void totalIgualValorVenta() {
            ComprobanteLineaDTO item = TestDataBuilder.itemInafecto(1, "Servicio",
                    new BigDecimal("1"), new BigDecimal("5000.00"));
            assertEquals(item.getValorVenta(), item.getMontoTotal());
        }
    }
}

