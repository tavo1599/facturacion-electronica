package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para Comunicación de Baja de comprobantes")
public class BajaRequestDTO {

    @NotNull(message = "Los datos de la empresa emisora son obligatorios")
    @Valid
    private EmpresaDTO empresa;

    @Schema(description = "Fecha de generación de la baja", example = "2026-04-16")
    private LocalDate fechaGeneracion;

    @Schema(description = "Fecha de emisión de los documentos dados de baja", example = "2026-04-16")
    @NotNull(message = "La fecha de baja de documentos es obligatoria")
    private LocalDate fechaBajaDocs;

    @Schema(description = "Correlativo de la comunicación de baja del día (1, 2, 3...)", example = "1")
    @NotNull(message = "El correlativo de baja es obligatorio")
    @Min(value = 1, message = "El correlativo mínimo es 1")
    private Integer correlativo;

    @Schema(description = "Lista de documentos a dar de baja")
    @NotEmpty(message = "Debe incluir al menos un documento a dar de baja")
    @Valid
    private List<BajaItemDTO> items;

    // --- Campos calculados ---

    /**
     * Nombre del archivo según nomenclatura SUNAT:
     * RA-{fechaGeneracion}-{correlativo}
     * Ejemplo: RA-20260416-1
     */
    public String getIdentificador() {
        LocalDate fecha = fechaGeneracion != null ? fechaGeneracion : LocalDate.now();
        return String.format("RA-%s-%d",
                fecha.toString().replace("-", ""), correlativo);
    }

    public String getNombreArchivo() {
        return String.format("%s-%s", empresa.getRuc(), getIdentificador());
    }

    @Data
    @Schema(description = "Documento a dar de baja")
    public static class BajaItemDTO {

        @Schema(description = "Número de orden en la baja", example = "1")
        @NotNull(message = "El número de orden es obligatorio")
        private Integer numero;

        @Schema(description = "Tipo de documento: 01=Factura, 07=NC, 08=ND", example = "01")
        @NotBlank(message = "El tipo de documento es obligatorio")
        private String tipoDocumento;

        @Schema(description = "Serie del documento", example = "F001")
        @NotBlank(message = "La serie es obligatoria")
        private String serie;

        @Schema(description = "Número correlativo del documento", example = "1")
        @NotNull(message = "El correlativo es obligatorio")
        private Integer correlativo;

        @Schema(description = "Motivo de la baja", example = "Error en datos del comprobante")
        @NotBlank(message = "El motivo de baja es obligatorio")
        private String motivo;

        public String getSerieCorrelativo() {
            return String.format("%s-%08d", serie, correlativo);
        }
    }
}

