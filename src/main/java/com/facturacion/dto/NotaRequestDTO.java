package com.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class NotaRequestDTO {

    // --- DATOS DEL EMISOR ---

    @NotNull(message = "Los datos de la empresa emisora son obligatorios")
    @Valid
    private EmpresaDTO empresa;

    // --- TIPO DE NOTA ---

    /**
     * Tipo de nota:
     * "07" = Nota de Crédito
     * "08" = Nota de Débito
     */
    @NotBlank(message = "El tipo de nota es obligatorio (07=Crédito, 08=Débito)")
    @Pattern(regexp = "07|08", message = "Tipo inválido. Use 07 (Nota Crédito) o 08 (Nota Débito)")
    private String tipoNota;

    /**
     * Serie de la nota.
     * Si modifica una Factura: F001, F002, etc.
     * Si modifica una Boleta: B001, B002, etc.
     */
    @NotBlank(message = "La serie es obligatoria")
    @Pattern(regexp = "[FB]\\w{3}", message = "Serie inválida. Ej: F001 o B001")
    private String serie;

    @NotNull(message = "El correlativo es obligatorio")
    @Min(value = 1)
    @Max(value = 99999999)
    private Integer correlativo;

    // --- DOCUMENTO QUE MODIFICA ---

    /**
     * Tipo del comprobante que se modifica:
     * "01" = Factura
     * "03" = Boleta
     */
    @NotBlank(message = "El tipo de comprobante afectado es obligatorio")
    @Pattern(regexp = "01|03", message = "Tipo de comprobante afectado inválido")
    private String tipoComprobanteAfectado;

    /**
     * Serie-correlativo del comprobante que se modifica.
     * Ej: "F001-00000001"
     */
    @NotBlank(message = "El comprobante afectado es obligatorio")
    private String comprobanteAfectado;

    // --- MOTIVO ---

    /**
     * Código de motivo (Catálogo 09 para NC, Catálogo 10 para ND):
     *
     * NOTA DE CRÉDITO (Catálogo 09):
     * "01" = Anulación de la operación
     * "02" = Anulación por error en el RUC
     * "03" = Corrección por error en la descripción
     * "04" = Descuento global
     * "05" = Descuento por ítem
     * "06" = Devolución total
     * "07" = Devolución por ítem
     * "08" = Bonificación
     * "09" = Disminución en el valor
     * "10" = Otros conceptos
     * "11" = Ajustes de operaciones de exportación
     * "12" = Ajustes afectos al IVAP
     * "13" = Corrección del monto neto pendiente de pago
     *
     * NOTA DE DÉBITO (Catálogo 10):
     * "01" = Intereses por mora
     * "02" = Aumento en el valor
     * "03" = Penalidades / otros conceptos
     * "10" = Ajustes de operaciones de exportación
     * "11" = Ajustes afectos al IVAP
     */
    @NotBlank(message = "El código de motivo es obligatorio")
    private String codigoMotivo;

    /**
     * Descripción del motivo
     */
    @NotBlank(message = "La descripción del motivo es obligatoria")
    private String descripcionMotivo;

    // --- FECHA ---

    private LocalDate fechaEmision;
    private String horaEmision;

    // --- TIPO OPERACIÓN Y MONEDA ---

    private String tipoOperacion = "0101";
    private String moneda = "PEN";

    // --- DATOS DEL CLIENTE ---

    @NotBlank(message = "El tipo de documento del cliente es obligatorio")
    private String clienteTipoDocumento;

    @NotBlank(message = "El número de documento del cliente es obligatorio")
    private String clienteNumeroDocumento;

    @NotBlank(message = "La razón social del cliente es obligatoria")
    private String clienteRazonSocial;

    private String clienteDireccion;

    // --- ITEMS ---

    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<ComprobanteLineaDTO> items;

    // --- CAMPOS CALCULADOS ---

    public String getSerieCorrelativo() {
        return String.format("%s-%08d", serie, correlativo);
    }

    public BigDecimal getTotalValorVenta() {
        return items.stream()
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalIgv() {
        return items.stream()
                .map(ComprobanteLineaDTO::getMontoIgv)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGravado() {
        return items.stream()
                .filter(i -> "10".equals(i.getTipoAfectacionIgv()))
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalExonerado() {
        return items.stream()
                .filter(i -> "20".equals(i.getTipoAfectacionIgv()))
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalInafecto() {
        return items.stream()
                .filter(i -> "30".equals(i.getTipoAfectacionIgv()))
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getImporteTotal() {
        return getTotalValorVenta().add(getTotalIgv());
    }

    public String getNombreArchivo() {
        return String.format("%s-%s-%s-%08d", empresa.getRuc(), tipoNota, serie, correlativo);
    }
}
