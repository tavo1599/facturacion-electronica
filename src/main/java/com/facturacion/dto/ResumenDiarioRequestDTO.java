package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para Resumen Diario de Boletas")
public class ResumenDiarioRequestDTO {

    @NotNull(message = "Los datos de la empresa emisora son obligatorios")
    @Valid
    private EmpresaDTO empresa;

    @Schema(description = "Fecha de generación del resumen", example = "2026-04-16")
    private LocalDate fechaGeneracion;

    @Schema(description = "Fecha de emisión de las boletas del resumen", example = "2026-04-16")
    @NotNull(message = "La fecha de resumen es obligatoria")
    private LocalDate fechaResumen;

    @Schema(description = "Correlativo del resumen del día (1, 2, 3...)", example = "1")
    @NotNull(message = "El correlativo es obligatorio")
    @Min(value = 1)
    private Integer correlativo;

    @Schema(description = "Moneda", example = "PEN")
    private String moneda = "PEN";

    @Schema(description = "Lista de boletas del resumen")
    @NotEmpty(message = "Debe incluir al menos una boleta")
    @Valid
    private List<ResumenItemDTO> items;

    // --- Campos calculados ---

    public String getIdentificador() {
        LocalDate fecha = fechaGeneracion != null ? fechaGeneracion : LocalDate.now();
        return String.format("RC-%s-%d",
                fecha.toString().replace("-", ""), correlativo);
    }

    public String getNombreArchivo() {
        return String.format("%s-%s", empresa.getRuc(), getIdentificador());
    }

    @Data
    @Schema(description = "Boleta incluida en el resumen diario")
    public static class ResumenItemDTO {

        @Schema(description = "Número de orden", example = "1")
        @NotNull private Integer numero;

        @Schema(description = "Tipo de documento: 03=Boleta", example = "03")
        @NotBlank private String tipoDocumento;

        @Schema(description = "Serie", example = "B001")
        @NotBlank private String serie;

        @Schema(description = "Correlativo inicio", example = "1")
        @NotNull private Integer correlativoInicio;

        @Schema(description = "Correlativo fin", example = "1")
        @NotNull private Integer correlativoFin;

        @Schema(description = "Tipo de operación: 1=Adicionar, 2=Modificar, 3=Anular", example = "1")
        @NotBlank private String tipoOperacion; // 1, 2, 3

        @Schema(description = "Tipo doc cliente: 1=DNI, 0=Sin doc", example = "1")
        private String clienteTipoDocumento = "0";

        @Schema(description = "Número doc cliente", example = "00000000")
        private String clienteNumeroDocumento = "00000000";

        @Schema(description = "Total gravado", example = "100.00")
        private BigDecimal totalGravado = BigDecimal.ZERO;

        @Schema(description = "Total exonerado", example = "0.00")
        private BigDecimal totalExonerado = BigDecimal.ZERO;

        @Schema(description = "Total inafecto", example = "0.00")
        private BigDecimal totalInafecto = BigDecimal.ZERO;

        @Schema(description = "Total otros cargos", example = "0.00")
        private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

        @Schema(description = "Total ISC", example = "0.00")
        private BigDecimal totalIsc = BigDecimal.ZERO;

        @Schema(description = "Total IGV", example = "18.00")
        private BigDecimal totalIgv = BigDecimal.ZERO;

        @Schema(description = "Importe total", example = "118.00")
        @NotNull private BigDecimal importeTotal;

        public String getSerieCorrelativo() {
            return String.format("%s-%08d", serie, correlativoInicio);
        }
    }
}

