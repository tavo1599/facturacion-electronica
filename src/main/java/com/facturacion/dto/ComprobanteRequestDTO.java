package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para emitir Factura o Boleta de Venta")
public class ComprobanteRequestDTO {

    // --- DATOS DEL EMISOR ---

    /**
     * Datos de la empresa emisora (RUC, razón social, dirección, credenciales SOL, etc.)
     */
    @NotNull(message = "Los datos de la empresa emisora son obligatorios")
    @Valid
    private EmpresaDTO empresa;

    // --- TIPO DE COMPROBANTE ---
    
    /**
     * Tipo de comprobante:
     * "01" = Factura
     * "03" = Boleta de Venta
     */
    @Schema(description = "Tipo de comprobante: 01=Factura, 03=Boleta", example = "01")
    @NotBlank(message = "El tipo de comprobante es obligatorio (01=Factura, 03=Boleta)")
    @Pattern(regexp = "01|03", message = "Tipo de comprobante inválido. Use 01 (Factura) o 03 (Boleta)")
    private String tipoComprobante;

    @Schema(description = "Serie del comprobante", example = "F001")
    @NotBlank(message = "La serie es obligatoria")
    @Pattern(regexp = "[FB]\\w{3}", message = "Serie inválida. Factura: F001, Boleta: B001")
    private String serie;

    @Schema(description = "Número correlativo", example = "1")
    @NotNull(message = "El correlativo es obligatorio")
    @Min(value = 1, message = "El correlativo mínimo es 1")
    @Max(value = 99999999, message = "El correlativo máximo es 99999999")
    private Integer correlativo;

    // --- FECHA ---
    
    @Schema(description = "Fecha de emisión", example = "2026-04-16")
    private LocalDate fechaEmision;

    @Schema(description = "Hora de emisión", example = "12:00:00")
    private String horaEmision;

    @Schema(description = "Fecha de vencimiento", example = "2026-05-16")
    private LocalDate fechaVencimiento;

    // --- TIPO DE OPERACIÓN ---
    
    @Schema(description = "Tipo de operación", example = "0101")
    private String tipoOperacion = "0101";

    @Schema(description = "Moneda ISO 4217: PEN o USD", example = "PEN")
    private String moneda = "PEN";

    @Schema(description = "Tipo doc cliente: 6=RUC, 1=DNI, 0=Sin doc", example = "6")
    @NotBlank(message = "El tipo de documento del cliente es obligatorio")
    private String clienteTipoDocumento;

    @Schema(description = "Número de documento del cliente", example = "20987654321")
    @NotBlank(message = "El número de documento del cliente es obligatorio")
    private String clienteNumeroDocumento;

    @Schema(description = "Razón social del cliente", example = "CLIENTE PRUEBA S.A.C.")
    @NotBlank(message = "La razón social del cliente es obligatoria")
    private String clienteRazonSocial;

    @Schema(description = "Dirección del cliente", example = "AV. AREQUIPA 456, LIMA")
    private String clienteDireccion;

    @Schema(description = "Forma de pago: Contado o Credito", example = "Contado")
    private String formaPago = "Contado";

    // --- LÍNEAS DE DETALLE ---
    
    @NotEmpty(message = "Debe incluir al menos una línea de detalle")
    @Valid
    private List<ComprobanteLineaDTO> items;

    // --- CAMPOS CALCULADOS ---

    /**
     * Genera el ID del comprobante: Serie-Correlativo (ej: F001-00000001)
     */
    public String getSerieCorrelativo() {
        return String.format("%s-%08d", serie, correlativo);
    }

    /**
     * Total valor venta (suma de valores sin IGV)
     */
    public BigDecimal getTotalValorVenta() {
        return items.stream()
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total IGV
     */
    public BigDecimal getTotalIgv() {
        return items.stream()
                .map(ComprobanteLineaDTO::getMontoIgv)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total gravado (items con IGV)
     */
    public BigDecimal getTotalGravado() {
        return items.stream()
                .filter(item -> "10".equals(item.getTipoAfectacionIgv()))
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total exonerado
     */
    public BigDecimal getTotalExonerado() {
        return items.stream()
                .filter(item -> "20".equals(item.getTipoAfectacionIgv()))
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total inafecto
     */
    public BigDecimal getTotalInafecto() {
        return items.stream()
                .filter(item -> "30".equals(item.getTipoAfectacionIgv()))
                .map(ComprobanteLineaDTO::getValorVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Importe total del comprobante (valor venta + IGV)
     */
    public BigDecimal getImporteTotal() {
        return getTotalValorVenta().add(getTotalIgv());
    }

    /**
     * Nombre del archivo XML según nomenclatura SUNAT:
     * RUC-TipoComprobante-Serie-Correlativo.xml
     */
    public String getNombreArchivo() {
        return String.format("%s-%s-%s-%08d", empresa.getRuc(), tipoComprobante, serie, correlativo);
    }

    // ============================================================
    // DETRACCIÓN SUNAT (opcional)
    // ============================================================

    @Schema(description = "¿Aplica detracción?", example = "false")
    private Boolean tieneDetraccion;

    @Schema(description = "Código detracción del Catálogo 54 SUNAT", example = "022")
    private String codigoDetraccion;

    @Schema(description = "Porcentaje de detracción", example = "12.00")
    private BigDecimal porcentajeDetraccion;

    @Schema(description = "Monto de detracción calculado", example = "120.00")
    private BigDecimal montoDetraccion;

    @Schema(description = "Saldo a pagar (importe total - detracción)", example = "880.00")
    private BigDecimal saldoAPagar;
}
