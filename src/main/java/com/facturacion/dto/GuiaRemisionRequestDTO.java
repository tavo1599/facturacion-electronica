package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para Guía de Remisión Electrónica")
public class GuiaRemisionRequestDTO {

    @NotNull(message = "Los datos de la empresa emisora son obligatorios")
    @Valid
    private EmpresaDTO empresa;

    @Schema(description = "Tipo de guía: 09=Remitente, 31=Transportista", example = "09")
    @NotBlank(message = "El tipo de guía es obligatorio")
    @Pattern(regexp = "09|31", message = "Tipo inválido. Use 09 (Remitente) o 31 (Transportista)")
    private String tipoGuia;

    @Schema(description = "Serie de la guía", example = "T001")
    @NotBlank(message = "La serie es obligatoria")
    @Pattern(regexp = "T\\w{3}", message = "Serie inválida. Ej: T001")
    private String serie;

    @Schema(description = "Número correlativo", example = "1")
    @NotNull @Min(1) @Max(99999999)
    private Integer correlativo;

    @Schema(description = "Fecha de emisión", example = "2026-04-16")
    private LocalDate fechaEmision;

    @Schema(description = "Fecha de inicio de traslado", example = "2026-04-17")
    @NotNull(message = "La fecha de inicio de traslado es obligatoria")
    private LocalDate fechaInicioTraslado;

    // --- MOTIVO DE TRASLADO ---

    @Schema(description = "Código motivo traslado (Cat. 20): 01=Venta, 02=Compra, 04=Traslado entre establecimientos, 13=Otros", example = "01")
    @NotBlank(message = "El motivo de traslado es obligatorio")
    private String motivoTraslado;

    @Schema(description = "Descripción del motivo", example = "Venta de mercadería")
    @NotBlank(message = "La descripción del motivo es obligatoria")
    private String descripcionMotivo;

    // --- MODALIDAD DE TRANSPORTE ---

    @Schema(description = "Modalidad transporte: 01=Público, 02=Privado", example = "02")
    @NotBlank(message = "La modalidad de transporte es obligatoria")
    @Pattern(regexp = "01|02", message = "Use 01 (Público) o 02 (Privado)")
    private String modalidadTransporte;

    // --- PESO ---

    @Schema(description = "Peso bruto total en KG", example = "50.00")
    @NotNull(message = "El peso bruto es obligatorio")
    private BigDecimal pesoBrutoTotal;

    @Schema(description = "Unidad de medida del peso", example = "KGM")
    private String unidadPeso = "KGM";

    // --- DESTINATARIO ---

    @Schema(description = "Tipo doc destinatario: 6=RUC, 1=DNI", example = "6")
    @NotBlank private String destinatarioTipoDocumento;

    @Schema(description = "Número doc destinatario", example = "20987654321")
    @NotBlank private String destinatarioNumeroDocumento;

    @Schema(description = "Razón social destinatario", example = "CLIENTE DESTINO S.A.C.")
    @NotBlank private String destinatarioRazonSocial;

    // --- PUNTO DE PARTIDA ---

    @Schema(description = "Ubigeo punto de partida", example = "150101")
    @NotBlank private String partidaUbigeo;

    @Schema(description = "Dirección punto de partida", example = "AV. LOS OLIVOS 123, LIMA")
    @NotBlank private String partidaDireccion;

    // --- PUNTO DE LLEGADA ---

    @Schema(description = "Ubigeo punto de llegada", example = "040101")
    @NotBlank private String llegadaUbigeo;

    @Schema(description = "Dirección punto de llegada", example = "AV. EJERCITO 456, AREQUIPA")
    @NotBlank private String llegadaDireccion;

    // --- TRANSPORTISTA (si modalidad = 01 Público) ---

    @Schema(description = "Tipo doc transportista", example = "6")
    private String transportistaTipoDocumento;

    @Schema(description = "Número doc transportista", example = "20111111111")
    private String transportistaNumeroDocumento;

    @Schema(description = "Razón social transportista", example = "TRANSPORTE RÁPIDO S.A.C.")
    private String transportistaRazonSocial;

    @Schema(description = "Número de placa (transporte privado)", example = "ABC-123")
    private String numeroPlaca;

    // --- CONDUCTOR (si modalidad = 02 Privado) ---

    @Schema(description = "Tipo doc conductor", example = "1")
    private String conductorTipoDocumento;

    @Schema(description = "DNI del conductor", example = "12345678")
    private String conductorNumeroDocumento;

    @Schema(description = "Nombre del conductor", example = "Juan Pérez García")
    private String conductorNombre;

    @Schema(description = "Licencia de conducir", example = "Q12345678")
    private String conductorLicencia;

    // --- DOCUMENTO RELACIONADO (opcional) ---

    @Schema(description = "Tipo doc relacionado: 01=Factura, 03=Boleta", example = "01")
    private String docRelacionadoTipo;

    @Schema(description = "Número doc relacionado", example = "F001-00000001")
    private String docRelacionadoNumero;

    // --- ITEMS ---

    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<GuiaItemDTO> items;

    // --- Calculados ---

    public String getSerieCorrelativo() {
        return String.format("%s-%08d", serie, correlativo);
    }

    public String getNombreArchivo() {
        return String.format("%s-%s-%s-%08d", empresa.getRuc(), tipoGuia, serie, correlativo);
    }

    @Data
    @Schema(description = "Ítem de la guía de remisión")
    public static class GuiaItemDTO {

        @Schema(description = "Número de orden", example = "1")
        @NotNull private Integer numero;

        @Schema(description = "Código del producto", example = "PROD001")
        @NotBlank private String codigoProducto;

        @Schema(description = "Descripción", example = "Laptop HP 15 pulgadas")
        @NotBlank private String descripcion;

        @Schema(description = "Unidad de medida", example = "NIU")
        @NotBlank private String unidadMedida;

        @Schema(description = "Cantidad", example = "5")
        @NotNull private BigDecimal cantidad;

        @Schema(description = "Peso unitario en KG", example = "2.50")
        private BigDecimal pesoUnitario;
    }
}

