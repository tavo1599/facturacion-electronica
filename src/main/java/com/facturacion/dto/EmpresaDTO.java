package com.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Datos del emisor (empresa)")
public class EmpresaDTO {

    @Schema(description = "RUC del emisor (11 dígitos)", example = "20123456789")
    @NotBlank(message = "El RUC del emisor es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
    private String ruc;

    @Schema(description = "Razón social del emisor", example = "MI EMPRESA S.A.C.")
    @NotBlank(message = "La razón social del emisor es obligatoria")
    private String razonSocial;

    @Schema(description = "Nombre comercial", example = "MI EMPRESA")
    private String nombreComercial;

    @Schema(description = "Dirección del emisor", example = "AV. LOS OLIVOS 123, LIMA")
    @NotBlank(message = "La dirección del emisor es obligatoria")
    private String direccion;

    @Schema(description = "Ubigeo (6 dígitos)", example = "150101")
    @NotBlank(message = "El ubigeo del emisor es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El ubigeo debe tener 6 dígitos")
    private String ubigeo;

    @Schema(description = "Departamento", example = "LIMA")
    @NotBlank(message = "El departamento es obligatorio")
    private String departamento;

    @Schema(description = "Provincia", example = "LIMA")
    @NotBlank(message = "La provincia es obligatoria")
    private String provincia;

    @Schema(description = "Distrito", example = "LIMA")
    @NotBlank(message = "El distrito es obligatorio")
    private String distrito;

    @Schema(description = "Código de país", example = "PE")
    private String codigoPais = "PE";

    @Schema(description = "Usuario SOL", example = "MODDATOS")
    @NotBlank(message = "El usuario SOL es obligatorio")
    private String solUsuario;

    @Schema(description = "Clave SOL", example = "MODDATOS")
    @NotBlank(message = "La clave SOL es obligatoria")
    private String solClave;

    @Schema(description = "Ambiente: beta o produccion", example = "beta")
    private String ambiente = "beta";

    // --- Credenciales OAuth2 (para Guías REST en producción) ---

    @Schema(description = "Client ID para API REST SUNAT (Guías)", example = "")
    private String clientId;

    @Schema(description = "Client Secret para API REST SUNAT (Guías)", example = "")
    private String clientSecret;

    // --- Detracción ---

    @Schema(description = "Cuenta detracción Banco de la Nación", example = "00-000-123456")
    private String cuentaDetraccionBN;

    // --- Helpers ---

    /**
     * Credencial SOL completa: RUC + Usuario
     */
    public String getSolUsername() {
        return ruc + solUsuario;
    }

    /**
     * Nombre comercial, si no se especifica usa razón social
     */
    public String getNombreComercialEffective() {
        return nombreComercial != null && !nombreComercial.isBlank()
                ? nombreComercial : razonSocial;
    }
}

