package com.facturacion.util;

import java.math.BigDecimal;

/**
 * Convierte montos numéricos a texto (requerido por SUNAT en las leyendas)
 * Ejemplo: 1500.50 -> "MIL QUINIENTOS CON 50/100 SOLES"
 */
public final class MontoEnLetras {

    private MontoEnLetras() {}

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO",
        "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ",
        "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
        "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };

    private static final String[] DECENAS = {
        "", "", "VEINTI", "TREINTA", "CUARENTA", "CINCUENTA",
        "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
        "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    public static String convertir(BigDecimal monto, String moneda) {
        long parteEntera = monto.longValue();
        int decimales = monto.remainder(BigDecimal.ONE)
                .movePointRight(2).intValue();

        String nombreMoneda = "PEN".equals(moneda) ? "SOLES" : "DOLARES AMERICANOS";
        String texto = convertirEntero(parteEntera);

        return texto + " CON " + String.format("%02d", decimales) + "/100 " + nombreMoneda;
    }

    private static String convertirEntero(long numero) {
        if (numero == 0) return "CERO";
        if (numero < 0) return "MENOS " + convertirEntero(-numero);

        StringBuilder resultado = new StringBuilder();

        if (numero >= 1000000) {
            long millones = numero / 1000000;
            if (millones == 1) {
                resultado.append("UN MILLON ");
            } else {
                resultado.append(convertirEntero(millones)).append(" MILLONES ");
            }
            numero %= 1000000;
        }

        if (numero >= 1000) {
            long miles = numero / 1000;
            if (miles == 1) {
                resultado.append("MIL ");
            } else {
                resultado.append(convertirEntero(miles)).append(" MIL ");
            }
            numero %= 1000;
        }

        if (numero >= 100) {
            if (numero == 100) {
                resultado.append("CIEN");
                return resultado.toString().trim();
            }
            resultado.append(CENTENAS[(int) (numero / 100)]).append(" ");
            numero %= 100;
        }

        if (numero > 0) {
            if (numero <= 20) {
                resultado.append(UNIDADES[(int) numero]);
            } else {
                int decena = (int) (numero / 10);
                int unidad = (int) (numero % 10);
                if (decena == 2 && unidad > 0) {
                    resultado.append("VEINTI").append(UNIDADES[unidad]);
                } else if (unidad > 0) {
                    resultado.append(DECENAS[decena]).append(" Y ").append(UNIDADES[unidad]);
                } else {
                    resultado.append(DECENAS[decena]);
                }
            }
        }

        return resultado.toString().trim();
    }
}
