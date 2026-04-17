package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para Comprobante de Percepción")
public class PercepcionRequestDTO {

    @NotNull @Valid
    private EmpresaDTO empresa;

    @Schema(description = "Serie de la percepción", example = "P001")
    @NotBlank @Pattern(regexp = "P\\w{3}", message = "Serie inválida. Ej: P001")
    private String serie;

    @Schema(description = "Número correlativo", example = "1")
    @NotNull @Min(1) @Max(99999999)
    private Integer correlativo;

    @Schema(description = "Fecha de emisión", example = "2026-04-16")
    private LocalDate fechaEmision;

    @Schema(description = "Régimen de percepción: 01=Venta interna 2%, 02=Adquisición combustible 1%, 03=Importación 3.5%", example = "01")
    @NotBlank private String regimenPercepcion;

    @Schema(description = "Tasa de percepción", example = "2.00")
    @NotNull private BigDecimal tasaPercepcion;

    @Schema(description = "Observaciones", example = "Percepción del periodo abril 2026")
    private String observaciones;

    @Schema(description = "Moneda", example = "PEN")
    private String moneda = "PEN";

    // --- Cliente (a quien se percibe) ---
    @Schema(description = "Tipo doc cliente: 6=RUC", example = "6")
    @NotBlank private String clienteTipoDocumento;

    @Schema(description = "Número doc cliente", example = "20987654321")
    @NotBlank private String clienteNumeroDocumento;

    @Schema(description = "Razón social cliente", example = "CLIENTE EJEMPLO S.A.C.")
    @NotBlank private String clienteRazonSocial;

    @Schema(description = "Dirección cliente", example = "AV. AREQUIPA 456, LIMA")
    private String clienteDireccion;

    // --- Items ---
    @NotEmpty @Valid
    private List<PercepcionItemDTO> items;

    // --- Calculados ---
    public String getSerieCorrelativo() {
        return String.format("%s-%08d", serie, correlativo);
    }

    public String getNombreArchivo() {
        return String.format("%s-40-%s-%08d", empresa.getRuc(), serie, correlativo);
    }

    public BigDecimal getTotalPercibido() {
        return items.stream()
                .map(PercepcionItemDTO::getMontoPercibido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCobrado() {
        return items.stream()
                .map(PercepcionItemDTO::getImporteCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Data
    @Schema(description = "Documento relacionado con la percepción")
    public static class PercepcionItemDTO {

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

        @Schema(description = "Fecha de cobro", example = "2026-04-15")
        @NotNull private LocalDate fechaCobro;

        @Schema(description = "Número de cobro", example = "001")
        @NotBlank private String numeroCobro;

        @Schema(description = "Importe cobrado (con percepción)", example = "1203.60")
        @NotNull private BigDecimal importeCobrado;

        @Schema(description = "Moneda de cobro", example = "PEN")
        private String monedaCobro = "PEN";

        @Schema(description = "Monto percibido", example = "23.60")
        @NotNull private BigDecimal montoPercibido;

        @Schema(description = "Moneda percepción", example = "PEN")
        private String monedaPercepcion = "PEN";

        @Schema(description = "Tipo de cambio", example = "1.00")
        private BigDecimal tipoCambio;
    }
}

