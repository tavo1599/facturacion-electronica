package com.facturacion.util;

import com.facturacion.dto.ComprobanteLineaDTO;
import com.facturacion.dto.ComprobanteRequestDTO;
import com.facturacion.dto.EmpresaDTO;
import com.facturacion.dto.NotaRequestDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder de datos de prueba para todos los escenarios de facturación electrónica SUNAT
 */
public final class TestDataBuilder {

    private TestDataBuilder() {}

    // ==================== EMPRESA DE PRUEBA ====================

    /** Empresa emisora de prueba */
    public static EmpresaDTO empresaTest() {
        EmpresaDTO empresa = new EmpresaDTO();
        empresa.setRuc("20123456789");
        empresa.setRazonSocial("MI EMPRESA S.A.C.");
        empresa.setNombreComercial("MI EMPRESA");
        empresa.setDireccion("AV. EJEMPLO 123");
        empresa.setUbigeo("210101");
        empresa.setDepartamento("PUNO");
        empresa.setProvincia("SAN ROMAN");
        empresa.setDistrito("JULIACA");
        empresa.setCodigoPais("PE");
        empresa.setSolUsuario("MODDATOS");
        empresa.setSolClave("MODDATOS");
        empresa.setAmbiente("beta");
        return empresa;
    }

    // ==================== ITEMS ====================

    /** Item gravado (IGV 18%) */
    public static ComprobanteLineaDTO itemGravado(int numero, String descripcion,
                                                   BigDecimal cantidad, BigDecimal valorUnitario) {
        ComprobanteLineaDTO item = new ComprobanteLineaDTO();
        item.setNumero(numero);
        item.setCodigoProducto("PROD" + numero);
        item.setUnidadMedida("NIU");
        item.setCantidad(cantidad);
        item.setDescripcion(descripcion);
        item.setValorUnitario(valorUnitario);
        // Precio con IGV = valor * 1.18
        item.setPrecioUnitario(valorUnitario.multiply(new BigDecimal("1.18"))
                .setScale(2, BigDecimal.ROUND_HALF_UP));
        item.setTipoPrecio("01");
        item.setTipoAfectacionIgv("10"); // Gravado
        item.setPorcentajeIgv(new BigDecimal("18.00"));
        item.setCodigoTributo("1000");
        return item;
    }

    /** Item exonerado (IGV 0%) */
    public static ComprobanteLineaDTO itemExonerado(int numero, String descripcion,
                                                     BigDecimal cantidad, BigDecimal valorUnitario) {
        ComprobanteLineaDTO item = new ComprobanteLineaDTO();
        item.setNumero(numero);
        item.setCodigoProducto("PROD" + numero);
        item.setUnidadMedida("NIU");
        item.setCantidad(cantidad);
        item.setDescripcion(descripcion);
        item.setValorUnitario(valorUnitario);
        item.setPrecioUnitario(valorUnitario); // Sin IGV
        item.setTipoPrecio("01");
        item.setTipoAfectacionIgv("20"); // Exonerado
        item.setPorcentajeIgv(BigDecimal.ZERO);
        item.setCodigoTributo("9997");
        return item;
    }

    /** Item inafecto (IGV 0%) */
    public static ComprobanteLineaDTO itemInafecto(int numero, String descripcion,
                                                    BigDecimal cantidad, BigDecimal valorUnitario) {
        ComprobanteLineaDTO item = new ComprobanteLineaDTO();
        item.setNumero(numero);
        item.setCodigoProducto("PROD" + numero);
        item.setUnidadMedida("NIU");
        item.setCantidad(cantidad);
        item.setDescripcion(descripcion);
        item.setValorUnitario(valorUnitario);
        item.setPrecioUnitario(valorUnitario); // Sin IGV
        item.setTipoPrecio("01");
        item.setTipoAfectacionIgv("30"); // Inafecto
        item.setPorcentajeIgv(BigDecimal.ZERO);
        item.setCodigoTributo("9998");
        return item;
    }

    // ==================== FACTURAS ====================

    /** Factura 100% gravada */
    public static ComprobanteRequestDTO facturaGravada() {
        ComprobanteRequestDTO dto = baseFactura();
        dto.setItems(Arrays.asList(
                itemGravado(1, "Laptop HP 15-ef2126wm", new BigDecimal("2"), new BigDecimal("1500.00")),
                itemGravado(2, "Mouse inalámbrico Logitech", new BigDecimal("5"), new BigDecimal("45.00"))
        ));
        return dto;
    }

    /** Factura 100% exonerada */
    public static ComprobanteRequestDTO facturaExonerada() {
        ComprobanteRequestDTO dto = baseFactura();
        dto.setCorrelativo(2);
        dto.setItems(Arrays.asList(
                itemExonerado(1, "Libro de Matemáticas (Exonerado IGV)", new BigDecimal("10"), new BigDecimal("50.00")),
                itemExonerado(2, "Cuaderno universitario A4", new BigDecimal("20"), new BigDecimal("8.00"))
        ));
        return dto;
    }

    /** Factura 100% inafecta */
    public static ComprobanteRequestDTO facturaInafecta() {
        ComprobanteRequestDTO dto = baseFactura();
        dto.setCorrelativo(3);
        dto.setItems(Arrays.asList(
                itemInafecto(1, "Servicio de exportación (Inafecto)", new BigDecimal("1"), new BigDecimal("5000.00")),
                itemInafecto(2, "Asesoría técnica exportación", new BigDecimal("1"), new BigDecimal("2000.00"))
        ));
        return dto;
    }

    /** Factura mixta: gravado + exonerado + inafecto */
    public static ComprobanteRequestDTO facturaMixta() {
        ComprobanteRequestDTO dto = baseFactura();
        dto.setCorrelativo(4);
        dto.setItems(Arrays.asList(
                itemGravado(1, "Computadora Desktop (Gravado)", new BigDecimal("1"), new BigDecimal("2500.00")),
                itemExonerado(2, "Libro técnico Java (Exonerado)", new BigDecimal("3"), new BigDecimal("80.00")),
                itemInafecto(3, "Servicio de capacitación (Inafecto)", new BigDecimal("1"), new BigDecimal("1200.00"))
        ));
        return dto;
    }

    /** Factura gravada en USD */
    public static ComprobanteRequestDTO facturaGravadaUSD() {
        ComprobanteRequestDTO dto = baseFactura();
        dto.setCorrelativo(5);
        dto.setMoneda("USD");
        dto.setItems(Arrays.asList(
                itemGravado(1, "Servicio de consultoría IT", new BigDecimal("1"), new BigDecimal("3000.00"))
        ));
        return dto;
    }

    private static ComprobanteRequestDTO baseFactura() {
        ComprobanteRequestDTO dto = new ComprobanteRequestDTO();
        dto.setEmpresa(empresaTest());
        dto.setTipoComprobante("01");
        dto.setSerie("F001");
        dto.setCorrelativo(1);
        dto.setFechaEmision(LocalDate.of(2025, 4, 16));
        dto.setHoraEmision("10:30:00");
        dto.setTipoOperacion("0101");
        dto.setMoneda("PEN");
        dto.setClienteTipoDocumento("6");
        dto.setClienteNumeroDocumento("20512345678");
        dto.setClienteRazonSocial("EMPRESA CLIENTE S.A.C.");
        dto.setClienteDireccion("AV. LIMA 456, LIMA");
        dto.setFormaPago("Contado");
        return dto;
    }

    // ==================== BOLETAS ====================

    /** Boleta 100% gravada */
    public static ComprobanteRequestDTO boletaGravada() {
        ComprobanteRequestDTO dto = baseBoleta();
        dto.setItems(Arrays.asList(
                itemGravado(1, "Zapatos deportivos Nike", new BigDecimal("1"), new BigDecimal("250.00")),
                itemGravado(2, "Medias deportivas", new BigDecimal("3"), new BigDecimal("15.00"))
        ));
        return dto;
    }

    /** Boleta 100% exonerada */
    public static ComprobanteRequestDTO boletaExonerada() {
        ComprobanteRequestDTO dto = baseBoleta();
        dto.setCorrelativo(2);
        dto.setItems(Arrays.asList(
                itemExonerado(1, "Libro escolar primaria (Exonerado)", new BigDecimal("5"), new BigDecimal("25.00"))
        ));
        return dto;
    }

    /** Boleta 100% inafecta */
    public static ComprobanteRequestDTO boletaInafecta() {
        ComprobanteRequestDTO dto = baseBoleta();
        dto.setCorrelativo(3);
        dto.setItems(Arrays.asList(
                itemInafecto(1, "Servicio educativo (Inafecto)", new BigDecimal("1"), new BigDecimal("500.00"))
        ));
        return dto;
    }

    /** Boleta mixta */
    public static ComprobanteRequestDTO boletaMixta() {
        ComprobanteRequestDTO dto = baseBoleta();
        dto.setCorrelativo(4);
        dto.setItems(Arrays.asList(
                itemGravado(1, "Celular Samsung Galaxy (Gravado)", new BigDecimal("1"), new BigDecimal("800.00")),
                itemExonerado(2, "Libro de cuentos (Exonerado)", new BigDecimal("2"), new BigDecimal("30.00")),
                itemInafecto(3, "Seguro SOAT (Inafecto)", new BigDecimal("1"), new BigDecimal("150.00"))
        ));
        return dto;
    }

    private static ComprobanteRequestDTO baseBoleta() {
        ComprobanteRequestDTO dto = new ComprobanteRequestDTO();
        dto.setEmpresa(empresaTest());
        dto.setTipoComprobante("03");
        dto.setSerie("B001");
        dto.setCorrelativo(1);
        dto.setFechaEmision(LocalDate.of(2025, 4, 16));
        dto.setHoraEmision("11:00:00");
        dto.setTipoOperacion("0101");
        dto.setMoneda("PEN");
        dto.setClienteTipoDocumento("1");
        dto.setClienteNumeroDocumento("45678912");
        dto.setClienteRazonSocial("JUAN PEREZ GARCIA");
        dto.setClienteDireccion("JR. CUSCO 789, JULIACA");
        dto.setFormaPago("Contado");
        return dto;
    }

    // ==================== NOTAS DE CRÉDITO ====================

    /** NC gravada - Anulación de operación */
    public static NotaRequestDTO notaCreditoGravada() {
        NotaRequestDTO dto = baseNotaCredito();
        dto.setItems(Arrays.asList(
                itemGravado(1, "Laptop HP 15-ef2126wm (devolución)", new BigDecimal("2"), new BigDecimal("1500.00"))
        ));
        return dto;
    }

    /** NC exonerada */
    public static NotaRequestDTO notaCreditoExonerada() {
        NotaRequestDTO dto = baseNotaCredito();
        dto.setCorrelativo(2);
        dto.setCodigoMotivo("06");
        dto.setDescripcionMotivo("Devolución total");
        dto.setItems(Arrays.asList(
                itemExonerado(1, "Libro de Matemáticas (devolución)", new BigDecimal("10"), new BigDecimal("50.00"))
        ));
        return dto;
    }

    /** NC inafecta */
    public static NotaRequestDTO notaCreditoInafecta() {
        NotaRequestDTO dto = baseNotaCredito();
        dto.setCorrelativo(3);
        dto.setCodigoMotivo("09");
        dto.setDescripcionMotivo("Disminución en el valor");
        dto.setItems(Arrays.asList(
                itemInafecto(1, "Servicio exportación (ajuste)", new BigDecimal("1"), new BigDecimal("1000.00"))
        ));
        return dto;
    }

    /** NC mixta */
    public static NotaRequestDTO notaCreditoMixta() {
        NotaRequestDTO dto = baseNotaCredito();
        dto.setCorrelativo(4);
        dto.setCodigoMotivo("01");
        dto.setDescripcionMotivo("Anulación de la operación");
        dto.setItems(Arrays.asList(
                itemGravado(1, "Producto gravado (anulación)", new BigDecimal("1"), new BigDecimal("500.00")),
                itemExonerado(2, "Producto exonerado (anulación)", new BigDecimal("1"), new BigDecimal("100.00")),
                itemInafecto(3, "Servicio inafecto (anulación)", new BigDecimal("1"), new BigDecimal("200.00"))
        ));
        return dto;
    }

    /** NC sobre boleta */
    public static NotaRequestDTO notaCreditoSobreBoleta() {
        NotaRequestDTO dto = new NotaRequestDTO();
        dto.setEmpresa(empresaTest());
        dto.setTipoNota("07");
        dto.setSerie("B001");
        dto.setCorrelativo(5);
        dto.setTipoComprobanteAfectado("03");
        dto.setComprobanteAfectado("B001-00000001");
        dto.setCodigoMotivo("01");
        dto.setDescripcionMotivo("Anulación de la operación");
        dto.setFechaEmision(LocalDate.of(2025, 4, 16));
        dto.setHoraEmision("14:00:00");
        dto.setMoneda("PEN");
        dto.setClienteTipoDocumento("1");
        dto.setClienteNumeroDocumento("45678912");
        dto.setClienteRazonSocial("JUAN PEREZ GARCIA");
        dto.setItems(Arrays.asList(
                itemGravado(1, "Zapatos deportivos (devolución)", new BigDecimal("1"), new BigDecimal("250.00"))
        ));
        return dto;
    }

    private static NotaRequestDTO baseNotaCredito() {
        NotaRequestDTO dto = new NotaRequestDTO();
        dto.setEmpresa(empresaTest());
        dto.setTipoNota("07");
        dto.setSerie("F001");
        dto.setCorrelativo(1);
        dto.setTipoComprobanteAfectado("01");
        dto.setComprobanteAfectado("F001-00000001");
        dto.setCodigoMotivo("01");
        dto.setDescripcionMotivo("Anulación de la operación");
        dto.setFechaEmision(LocalDate.of(2025, 4, 16));
        dto.setHoraEmision("12:00:00");
        dto.setMoneda("PEN");
        dto.setClienteTipoDocumento("6");
        dto.setClienteNumeroDocumento("20512345678");
        dto.setClienteRazonSocial("EMPRESA CLIENTE S.A.C.");
        return dto;
    }

    // ==================== NOTAS DE DÉBITO ====================

    /** ND gravada - Intereses por mora */
    public static NotaRequestDTO notaDebitoGravada() {
        NotaRequestDTO dto = baseNotaDebito();
        dto.setItems(Arrays.asList(
                itemGravado(1, "Intereses por mora - Factura F001-00000001", new BigDecimal("1"), new BigDecimal("150.00"))
        ));
        return dto;
    }

    /** ND exonerada */
    public static NotaRequestDTO notaDebitoExonerada() {
        NotaRequestDTO dto = baseNotaDebito();
        dto.setCorrelativo(2);
        dto.setCodigoMotivo("02");
        dto.setDescripcionMotivo("Aumento en el valor");
        dto.setItems(Arrays.asList(
                itemExonerado(1, "Ajuste precio libro (aumento)", new BigDecimal("10"), new BigDecimal("5.00"))
        ));
        return dto;
    }

    /** ND inafecta */
    public static NotaRequestDTO notaDebitoInafecta() {
        NotaRequestDTO dto = baseNotaDebito();
        dto.setCorrelativo(3);
        dto.setCodigoMotivo("03");
        dto.setDescripcionMotivo("Penalidades");
        dto.setItems(Arrays.asList(
                itemInafecto(1, "Penalidad por incumplimiento", new BigDecimal("1"), new BigDecimal("500.00"))
        ));
        return dto;
    }

    /** ND mixta */
    public static NotaRequestDTO notaDebitoMixta() {
        NotaRequestDTO dto = baseNotaDebito();
        dto.setCorrelativo(4);
        dto.setCodigoMotivo("02");
        dto.setDescripcionMotivo("Aumento en el valor");
        dto.setItems(Arrays.asList(
                itemGravado(1, "Ajuste gravado", new BigDecimal("1"), new BigDecimal("300.00")),
                itemExonerado(2, "Ajuste exonerado", new BigDecimal("1"), new BigDecimal("100.00")),
                itemInafecto(3, "Ajuste inafecto", new BigDecimal("1"), new BigDecimal("50.00"))
        ));
        return dto;
    }

    /** ND sobre boleta */
    public static NotaRequestDTO notaDebitoSobreBoleta() {
        NotaRequestDTO dto = new NotaRequestDTO();
        dto.setEmpresa(empresaTest());
        dto.setTipoNota("08");
        dto.setSerie("B001");
        dto.setCorrelativo(5);
        dto.setTipoComprobanteAfectado("03");
        dto.setComprobanteAfectado("B001-00000001");
        dto.setCodigoMotivo("01");
        dto.setDescripcionMotivo("Intereses por mora");
        dto.setFechaEmision(LocalDate.of(2025, 4, 16));
        dto.setHoraEmision("15:00:00");
        dto.setMoneda("PEN");
        dto.setClienteTipoDocumento("1");
        dto.setClienteNumeroDocumento("45678912");
        dto.setClienteRazonSocial("JUAN PEREZ GARCIA");
        dto.setItems(Arrays.asList(
                itemGravado(1, "Intereses mora boleta", new BigDecimal("1"), new BigDecimal("25.00"))
        ));
        return dto;
    }

    private static NotaRequestDTO baseNotaDebito() {
        NotaRequestDTO dto = new NotaRequestDTO();
        dto.setEmpresa(empresaTest());
        dto.setTipoNota("08");
        dto.setSerie("F001");
        dto.setCorrelativo(1);
        dto.setTipoComprobanteAfectado("01");
        dto.setComprobanteAfectado("F001-00000001");
        dto.setCodigoMotivo("01");
        dto.setDescripcionMotivo("Intereses por mora");
        dto.setFechaEmision(LocalDate.of(2025, 4, 16));
        dto.setHoraEmision("13:00:00");
        dto.setMoneda("PEN");
        dto.setClienteTipoDocumento("6");
        dto.setClienteNumeroDocumento("20512345678");
        dto.setClienteRazonSocial("EMPRESA CLIENTE S.A.C.");
        return dto;
    }
}

