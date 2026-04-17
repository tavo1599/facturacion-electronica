package com.facturacion.util;

import com.facturacion.dto.ComprobanteLineaDTO;
import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.NotaRequestDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador de reglas de negocio SUNAT para comprobantes electrónicos UBL 2.1
 * Basado en:
 * - Resolución de Superintendencia N° 340-2017/SUNAT
 * - Anexo N° 8: Reglas de validación CPE
 * - Catálogos SUNAT vigentes
 */
public final class SunatValidator {

    private SunatValidator() {}

    /**
     * Valida un comprobante (Factura/Boleta) según reglas SUNAT.
     * Lanza IllegalArgumentException si hay errores.
     */
    public static List<String> validarComprobante(ComprobanteRequestDTO req) {
        List<String> errores = new ArrayList<>();

        // === Validar tipo comprobante y serie ===
        if ("01".equals(req.getTipoComprobante())) {
            if (!req.getSerie().startsWith("F")) {
                errores.add("FACTURA: La serie debe empezar con 'F' (ej: F001). Recibido: " + req.getSerie());
            }
            // Factura requiere RUC del receptor (tipo documento = 6)
            if (!"6".equals(req.getClienteTipoDocumento())) {
                errores.add("FACTURA: El tipo de documento del receptor debe ser '6' (RUC)");
            }
            // RUC debe tener 11 dígitos
            if (req.getClienteNumeroDocumento() == null || req.getClienteNumeroDocumento().length() != 11) {
                errores.add("FACTURA: El RUC del receptor debe tener 11 dígitos");
            }
            // RUC debe empezar con 10, 15, 17 o 20
            if (req.getClienteNumeroDocumento() != null && req.getClienteNumeroDocumento().length() == 11) {
                String prefix = req.getClienteNumeroDocumento().substring(0, 2);
                if (!prefix.equals("10") && !prefix.equals("15") && !prefix.equals("17") && !prefix.equals("20")) {
                    errores.add("FACTURA: El RUC del receptor debe empezar con 10, 15, 17 o 20. Recibido: " + prefix);
                }
            }
        } else if ("03".equals(req.getTipoComprobante())) {
            if (!req.getSerie().startsWith("B")) {
                errores.add("BOLETA: La serie debe empezar con 'B' (ej: B001). Recibido: " + req.getSerie());
            }
            // Boleta NO puede tener RUC como tipo de documento receptor
            // (Excepto si es > S/ 700 donde se exige DNI)
        }

        // === Validar tipo documento receptor ===
        if (!UblConstants.TIPOS_DOCUMENTO_VALIDOS.contains(req.getClienteTipoDocumento())) {
            errores.add("Tipo de documento '" + req.getClienteTipoDocumento()
                    + "' no válido según Catálogo 06 SUNAT");
        }

        // === Validar DNI de 8 dígitos ===
        if ("1".equals(req.getClienteTipoDocumento())) {
            if (req.getClienteNumeroDocumento() == null || req.getClienteNumeroDocumento().length() != 8) {
                errores.add("DNI debe tener 8 dígitos. Recibido: " + req.getClienteNumeroDocumento());
            }
        }

        // === Validar moneda ===
        if (req.getMoneda() != null && !req.getMoneda().matches("PEN|USD|EUR")) {
            errores.add("Moneda no soportada: " + req.getMoneda() + ". Use PEN, USD o EUR");
        }

        // === Validar items ===
        if (req.getItems() != null) {
            for (ComprobanteLineaDTO item : req.getItems()) {
                validarItem(item, errores);
            }

            // Validar cálculos de totales
            validarTotalesComprobante(req, errores);
        }

        return errores;
    }

    /**
     * Valida una nota (NC/ND) según reglas SUNAT
     */
    public static List<String> validarNota(NotaRequestDTO req) {
        List<String> errores = new ArrayList<>();

        // Serie debe coincidir con tipo de comprobante afectado
        if ("01".equals(req.getTipoComprobanteAfectado()) && !req.getSerie().startsWith("F")) {
            errores.add("Si modifica una FACTURA, la serie debe empezar con 'F'. Recibido: " + req.getSerie());
        }
        if ("03".equals(req.getTipoComprobanteAfectado()) && !req.getSerie().startsWith("B")) {
            errores.add("Si modifica una BOLETA, la serie debe empezar con 'B'. Recibido: " + req.getSerie());
        }

        // Formato comprobante afectado
        if (req.getComprobanteAfectado() != null && !req.getComprobanteAfectado().matches("[FB]\\w{3}-\\d{1,8}")) {
            errores.add("Formato comprobante afectado inválido: " + req.getComprobanteAfectado()
                    + ". Esperado: F001-00000001");
        }

        // Motivo según catálogo
        if ("07".equals(req.getTipoNota())) {
            String[] validosNC = {"01","02","03","04","05","06","07","08","09","10","11","12","13"};
            if (!containsValue(validosNC, req.getCodigoMotivo())) {
                errores.add("Código motivo '" + req.getCodigoMotivo()
                        + "' no válido para NC (Catálogo 09). Válidos: 01-13");
            }
        } else if ("08".equals(req.getTipoNota())) {
            String[] validosND = {"01","02","03","10","11"};
            if (!containsValue(validosND, req.getCodigoMotivo())) {
                errores.add("Código motivo '" + req.getCodigoMotivo()
                        + "' no válido para ND (Catálogo 10). Válidos: 01,02,03,10,11");
            }
        }

        // Factura requiere RUC del receptor
        if ("01".equals(req.getTipoComprobanteAfectado()) && !"6".equals(req.getClienteTipoDocumento())) {
            errores.add("Si modifica una FACTURA, el cliente debe tener tipo documento '6' (RUC)");
        }

        // Validar items
        if (req.getItems() != null) {
            for (ComprobanteLineaDTO item : req.getItems()) {
                validarItem(item, errores);
            }
        }

        return errores;
    }

    /**
     * Valida un item/línea de detalle según reglas SUNAT
     */
    public static void validarItem(ComprobanteLineaDTO item, List<String> errores) {
        String prefix = "Línea " + item.getNumero() + ": ";

        // Unidad de medida válida
        if (!UblConstants.UNIDADES_MEDIDA_VALIDAS.contains(item.getUnidadMedida())) {
            errores.add(prefix + "Unidad de medida '" + item.getUnidadMedida()
                    + "' no es válida según UN/ECE Rec 20 (SUNAT). "
                    + "Use NIU (unidad), ZZ (servicio), KGM (kg), BX (caja), etc.");
        }

        // Tipo de afectación IGV válido
        String tipoAfect = item.getTipoAfectacionIgv();
        if (tipoAfect != null) {
            try {
                int code = Integer.parseInt(tipoAfect);
                if (!((code >= 10 && code <= 17) || (code >= 20 && code <= 21)
                        || (code >= 30 && code <= 36) || code == 40)) {
                    errores.add(prefix + "Tipo afectación IGV '" + tipoAfect
                            + "' no válido según Catálogo 07 SUNAT");
                }
            } catch (NumberFormatException e) {
                errores.add(prefix + "Tipo afectación IGV debe ser numérico: " + tipoAfect);
            }
        }

        // Validar coherencia valorUnitario vs precioUnitario
        if (item.getValorUnitario() != null && item.getPrecioUnitario() != null) {
            if ("10".equals(tipoAfect)) {
                // Gravado: precioUnitario ≈ valorUnitario * 1.18
                BigDecimal esperado = item.getValorUnitario()
                        .multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal diff = esperado.subtract(item.getPrecioUnitario()).abs();
                if (diff.compareTo(new BigDecimal("0.02")) > 0) {
                    errores.add(prefix + "Gravado: precioUnitario (" + item.getPrecioUnitario()
                            + ") debería ser ≈ valorUnitario*1.18 (" + esperado + ")");
                }
            } else if ("20".equals(tipoAfect) || "30".equals(tipoAfect)) {
                // Exonerado/Inafecto: precioUnitario = valorUnitario (sin IGV)
                if (item.getValorUnitario().compareTo(item.getPrecioUnitario()) != 0) {
                    errores.add(prefix + "Exonerado/Inafecto: precioUnitario ("
                            + item.getPrecioUnitario() + ") debe ser igual a valorUnitario ("
                            + item.getValorUnitario() + ") ya que no aplica IGV");
                }
            }
        }

        // Validar porcentaje IGV
        if ("10".equals(tipoAfect) && item.getPorcentajeIgv() != null) {
            if (item.getPorcentajeIgv().compareTo(new BigDecimal("18.00")) != 0) {
                errores.add(prefix + "Porcentaje IGV para gravado debe ser 18.00%. Recibido: "
                        + item.getPorcentajeIgv());
            }
        }
        if (("20".equals(tipoAfect) || "30".equals(tipoAfect)) && item.getPorcentajeIgv() != null) {
            if (item.getPorcentajeIgv().compareTo(BigDecimal.ZERO) != 0) {
                errores.add(prefix + "Porcentaje IGV para exonerado/inafecto debe ser 0%. Recibido: "
                        + item.getPorcentajeIgv());
            }
        }

        // Código tributo coherente con tipo afectación
        if (tipoAfect != null && item.getCodigoTributo() != null) {
            String tributoEsperado;
            switch (tipoAfect.substring(0, 1)) {
                case "2": tributoEsperado = "9997"; break;
                case "3": tributoEsperado = "9998"; break;
                default:  tributoEsperado = "1000"; break;
            }
            if (!tributoEsperado.equals(item.getCodigoTributo())) {
                errores.add(prefix + "Código tributo '" + item.getCodigoTributo()
                        + "' no corresponde a tipo afectación '" + tipoAfect
                        + "'. Esperado: " + tributoEsperado);
            }
        }

        // Cantidad positiva
        if (item.getCantidad() != null && item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            errores.add(prefix + "La cantidad debe ser mayor a 0");
        }

        // Valor unitario no negativo
        if (item.getValorUnitario() != null && item.getValorUnitario().compareTo(BigDecimal.ZERO) < 0) {
            errores.add(prefix + "El valor unitario no puede ser negativo");
        }
    }

    /**
     * Valida que los totales del comprobante cuadren con los items
     */
    private static void validarTotalesComprobante(ComprobanteRequestDTO req, List<String> errores) {
        BigDecimal sumValorVenta = BigDecimal.ZERO;
        BigDecimal sumIgv = BigDecimal.ZERO;

        for (ComprobanteLineaDTO item : req.getItems()) {
            sumValorVenta = sumValorVenta.add(item.getValorVenta());
            sumIgv = sumIgv.add(item.getMontoIgv());
        }

        // Verificar que el total valor venta cuadre
        if (sumValorVenta.compareTo(req.getTotalValorVenta()) != 0) {
            errores.add("Total valor venta calculado (" + sumValorVenta
                    + ") no cuadra con la suma de items (" + req.getTotalValorVenta() + ")");
        }

        // Verificar que importe total = valor venta + IGV
        BigDecimal importeEsperado = sumValorVenta.add(sumIgv);
        if (importeEsperado.compareTo(req.getImporteTotal()) != 0) {
            errores.add("Importe total (" + req.getImporteTotal()
                    + ") debería ser valorVenta + IGV = " + importeEsperado);
        }
    }

    private static boolean containsValue(String[] arr, String value) {
        for (String v : arr) {
            if (v.equals(value)) return true;
        }
        return false;
    }
}

