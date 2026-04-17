package com.facturacion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Facturación Electrónica SUNAT - API")
                        .version("1.0.0")
                        .description("""
                                Sistema de Facturación Electrónica para SUNAT Perú - UBL 2.1
                                
                                **Funcionalidades:**
                                - Emisión de Facturas y Boletas
                                - Emisión de Notas de Crédito y Débito
                                - Firma digital XML con certificado .pfx/.p12
                                - Envío automático a SUNAT (beta y producción)
                                - Consulta y descarga de XML, ZIP y CDR
                                - Gestión de certificados digitales por empresa
                                
                                **Ambientes:** beta (pruebas) y produccion
                                """)
                        .contact(new Contact()
                                .name("Facturación Electrónica")
                                .email("soporte@facturacion.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://e-beta.sunat.gob.pe").description("SUNAT Beta")
                ));
    }
}

