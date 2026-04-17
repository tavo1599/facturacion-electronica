package com.facturacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para MontoEnLetras
 */
class MontoEnLetrasTest {

    @Test
    @DisplayName("Cero soles")
    void cero() {
        assertEquals("CERO CON 00/100 SOLES", MontoEnLetras.convertir(BigDecimal.ZERO, "PEN"));
    }

    @Test
    @DisplayName("Monto simple con decimales")
    void simple() {
        String result = MontoEnLetras.convertir(new BigDecimal("100.50"), "PEN");
        assertEquals("CIEN CON 50/100 SOLES", result);
    }

    @Test
    @DisplayName("Mil quinientos con 50 centavos")
    void milQuinientos() {
        String result = MontoEnLetras.convertir(new BigDecimal("1500.50"), "PEN");
        assertEquals("MIL QUINIENTOS CON 50/100 SOLES", result);
    }

    @Test
    @DisplayName("Moneda USD")
    void usd() {
        String result = MontoEnLetras.convertir(new BigDecimal("250.00"), "USD");
        assertTrue(result.contains("DOLARES AMERICANOS"));
    }

    @Test
    @DisplayName("Un millón")
    void millon() {
        String result = MontoEnLetras.convertir(new BigDecimal("1000000.00"), "PEN");
        assertTrue(result.contains("UN MILLON"));
    }

    @Test
    @DisplayName("Veintitres")
    void veintitres() {
        String result = MontoEnLetras.convertir(new BigDecimal("23.00"), "PEN");
        assertEquals("VEINTITRES CON 00/100 SOLES", result);
    }

    @Test
    @DisplayName("Treinta y uno")
    void treintayuno() {
        String result = MontoEnLetras.convertir(new BigDecimal("31.00"), "PEN");
        assertEquals("TREINTA Y UNO CON 00/100 SOLES", result);
    }

    @Test
    @DisplayName("Monto factura típica")
    void facturaTypica() {
        String result = MontoEnLetras.convertir(new BigDecimal("3805.50"), "PEN");
        assertTrue(result.contains("TRES MIL"));
        assertTrue(result.contains("SOLES"));
    }
}

