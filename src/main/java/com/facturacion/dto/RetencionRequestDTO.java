package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para Comprobante de Retención")
public class RetencionRequestDTO {

    @NotNull @Valid
    private EmpresaDTO empresa;

    @Schema(description = "Serie de la retención", example = "R001")
    @NotBlank @Pattern(regexp = "R\\w{3}", message = "Serie inválida. Ej: R001")
    private String serie;

    @Schema(description = "Número correlativo", example = "1")
    @NotNull @Min(1) @Max(99999999)
    private Integer correlativo;

    @Schema(description = "Fecha de emisión", example = "2026-04-16")
    private LocalDate fechaEmision;

    @Schema(description = "Régimen de retención: 01=Tasa 3%, 02=Tasa 6%", example = "01")
    @NotBlank private String regimenRetencion; // 01=3%, 02=6%

    @Schema(description = "Tasa de retención", example = "3.00")
    @NotNull private BigDecimal tasaRetencion;

    @Schema(description = "Observaciones", example = "Retención del periodo abril 2026")
    private String observaciones;

    @Schema(description = "Moneda", example = "PEN")
    private String moneda = "PEN";

    // --- Proveedor (a quien se retiene) ---
    @Schema(description = "Tipo doc proveedor: 6=RUC", example = "6")
    @NotBlank private String proveedorTipoDocumento;

    @Schema(description = "Número doc proveedor", example = "20987654321")
    @NotBlank private String proveedorNumeroDocumento;

    @Schema(description = "Razón social proveedor", example = "PROVEEDOR EJEMPLO S.A.C.")
    @NotBlank private String proveedorRazonSocial;

    @Schema(description = "Dirección proveedor", example = "AV. AREQUIPA 456, LIMA")
    private String proveedorDireccion;

    // --- Items ---
    @NotEmpty @Valid
    private List<RetencionItemDTO> items;

    // --- Calculados ---
    public String getSerieCorrelativo() {
        return String.format("%s-%08d", serie, correlativo);
    }

    public String getNombreArchivo() {
        return String.format("%s-20-%s-%08d", empresa.getRuc(), serie, correlativo);
    }

    public BigDecimal getTotalRetenido() {
        return items.stream()
                .map(RetencionItemDTO::getMontoRetenido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPagado() {
        return items.stream()
                .map(RetencionItemDTO::getImportePagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Data
    @Schema(description = "Documento relacionado con la retención")
    public static class RetencionItemDTO {

        @Schema(description = "Número de orden", example = "1")
        @NotNull private Integer numero;

        @Schema(description = "Tipo doc relacionado: 01=Factura", example = "01")
        @NotBlank private String tipoDocRelacionado;

        @Schema(description = "Serie-correlativo doc", example = "F001-00000001")
        @NotBlank private String numDocRelacionado;

        @Schema(description = "Fecha de emisión del doc", example = "2026-04-10")
        @NotNull private LocalDate fechaDocRelacionado;

        @Schema(description = "Importe total del documento", example = "1180.00")
        @NotNull private BigDecimal importeDocRelacionado;

        @Schema(description = "Moneda del documento", example = "PEN")
        private String monedaDocRelacionado = "PEN";

        @Schema(description = "Fecha de pago", example = "2026-04-15")
        @NotNull private LocalDate fechaPago;

        @Schema(description = "Número de pago", example = "001")
        @NotBlank private String numeroPago;

        @Schema(description = "Importe pagado (sin retención)", example = "1145.60")
        @NotNull private BigDecimal importePagado;

        @Schema(description = "Moneda de pago", example = "PEN")
        private String monedaPago = "PEN";

        @Schema(description = "Monto retenido", example = "34.40")
        @NotNull private BigDecimal montoRetenido;

        @Schema(description = "Moneda retención", example = "PEN")
        private String monedaRetencion = "PEN";

        @Schema(description = "Tipo de cambio (si moneda diferente)", example = "1.00")
        private BigDecimal tipoCambio;
    }
}

