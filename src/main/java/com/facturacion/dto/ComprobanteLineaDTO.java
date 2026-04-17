package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Schema(description = "Línea de detalle del comprobante")
public class ComprobanteLineaDTO {

    @Schema(description = "Número de línea", example = "1")
    @NotNull(message = "El número de línea es obligatorio")
    private Integer numero;

    @Schema(description = "Código del producto", example = "PROD001")
    @NotBlank(message = "El código del producto es obligatorio")
    private String codigoProducto;

    @Schema(description = "Unidad de medida: NIU, ZZ, KGM, LTR", example = "NIU")
    @NotBlank(message = "La unidad de medida es obligatoria")
    private String unidadMedida;

    @Schema(description = "Cantidad", example = "2")
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    @Schema(description = "Descripción del producto/servicio", example = "Producto de prueba gravado")
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @Schema(description = "Valor unitario sin IGV", example = "100.00")
    @NotNull(message = "El valor unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El valor unitario no puede ser negativo")
    private BigDecimal valorUnitario;

    @Schema(description = "Precio unitario con IGV", example = "118.00")
    @NotNull(message = "El precio unitario es obligatorio")
    private BigDecimal precioUnitario;

    @Schema(description = "Tipo de precio: 01=Con IGV", example = "01")
    private String tipoPrecio = "01";

    @Schema(description = "Tipo afectación IGV: 10=Gravado, 20=Exonerado, 30=Inafecto", example = "10")
    private String tipoAfectacionIgv = "10";

    @Schema(description = "Porcentaje de IGV", example = "18.00")
    private BigDecimal porcentajeIgv = new BigDecimal("18.00");

    @Schema(description = "Código tributo: 1000=IGV, 9997=Exonerado, 9998=Inafecto", example = "1000")
    private String codigoTributo = "1000";

    // --- Campos calculados ---
    
    public BigDecimal getValorVenta() {
        return valorUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMontoIgv() {
        if ("10".equals(tipoAfectacionIgv)) {
            return getValorVenta().multiply(porcentajeIgv)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getMontoTotal() {
        return getValorVenta().add(getMontoIgv());
    }
}
