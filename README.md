# Facturación Electrónica SUNAT - UBL 2.1
## Sistema con Spring Boot para Perú

### Requisitos
- Java 17+
- Maven 3.8+
- Certificado digital .pfx o .p12 (de prueba o real)

---

### Instalación

```bash
# 1. Generar certificado de pruebas
keytool -genkeypair -alias miempresa -keyalg RSA -keysize 2048 \
  -validity 365 -storetype PKCS12 \
  -keystore src/main/resources/certificates/certificado-prueba.pfx \
  -storepass miPassword123 \
  -dname "CN=Noe Wilber Tipo Mamani, OU=Sistemas, O=Mi Empresa SAC, L=Juliaca, ST=Puno, C=PE"

# 2. Compilar
mvn clean package -DskipTests

# 3. Ejecutar
mvn spring-boot:run
```

---

### Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/comprobantes/emitir` | Emite comprobante y envía a SUNAT |
| POST | `/api/comprobantes/generar-xml` | Solo genera XML (sin enviar) |
| POST | `/api/comprobantes/emitir-lote` | Emite varios comprobantes |
| GET | `/api/comprobantes/health` | Health check |

---

### Ejemplo 1: FACTURA (tipo 01)

```bash
curl -X POST http://localhost:8080/api/comprobantes/emitir \
  -H "Content-Type: application/json" \
  -d '{
    "tipoComprobante": "01",
    "serie": "F001",
    "correlativo": 1,
    "fechaEmision": "2026-04-16",
    "horaEmision": "10:30:00",
    "tipoOperacion": "0101",
    "moneda": "PEN",
    "formaPago": "Contado",
    "clienteTipoDocumento": "6",
    "clienteNumeroDocumento": "20512345678",
    "clienteRazonSocial": "EMPRESA CLIENTE S.A.C.",
    "clienteDireccion": "AV. LIMA 456, JULIACA",
    "items": [
        {
            "numero": 1,
            "codigoProducto": "P001",
            "unidadMedida": "NIU",
            "cantidad": 10,
            "descripcion": "LAPTOP HP PAVILION 15",
            "valorUnitario": 2542.37,
            "precioUnitario": 3000.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        },
        {
            "numero": 2,
            "codigoProducto": "P002",
            "unidadMedida": "NIU",
            "cantidad": 5,
            "descripcion": "MOUSE INALAMBRICO LOGITECH",
            "valorUnitario": 42.37,
            "precioUnitario": 50.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        },
        {
            "numero": 3,
            "codigoProducto": "P003",
            "unidadMedida": "NIU",
            "cantidad": 5,
            "descripcion": "TECLADO USB GENIUS",
            "valorUnitario": 25.42,
            "precioUnitario": 30.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        }
    ]
}'
```

---

### Ejemplo 2: BOLETA DE VENTA (tipo 03)

```bash
curl -X POST http://localhost:8080/api/comprobantes/emitir \
  -H "Content-Type: application/json" \
  -d '{
    "tipoComprobante": "03",
    "serie": "B001",
    "correlativo": 1,
    "fechaEmision": "2026-04-16",
    "tipoOperacion": "0101",
    "moneda": "PEN",
    "formaPago": "Contado",
    "clienteTipoDocumento": "1",
    "clienteNumeroDocumento": "45678912",
    "clienteRazonSocial": "JUAN PEREZ GARCIA",
    "clienteDireccion": "JR. MOQUEGUA 789, JULIACA",
    "items": [
        {
            "numero": 1,
            "codigoProducto": "S001",
            "unidadMedida": "ZZ",
            "cantidad": 1,
            "descripcion": "SERVICIO DE REPARACION DE COMPUTADORA",
            "valorUnitario": 84.75,
            "precioUnitario": 100.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        },
        {
            "numero": 2,
            "codigoProducto": "R001",
            "unidadMedida": "NIU",
            "cantidad": 2,
            "descripcion": "MEMORIA RAM DDR4 8GB",
            "valorUnitario": 127.12,
            "precioUnitario": 150.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        }
    ]
}'
```

---

### Ejemplo 3: Solo generar XML (sin enviar a SUNAT)

```bash
curl -X POST http://localhost:8080/api/comprobantes/generar-xml \
  -H "Content-Type: application/json" \
  -d '{
    "tipoComprobante": "01",
    "serie": "F001",
    "correlativo": 2,
    "tipoOperacion": "0101",
    "moneda": "PEN",
    "formaPago": "Contado",
    "clienteTipoDocumento": "6",
    "clienteNumeroDocumento": "20600123456",
    "clienteRazonSocial": "DISTRIBUIDORA ANDINA E.I.R.L.",
    "items": [
        {
            "numero": 1,
            "codigoProducto": "SERV01",
            "unidadMedida": "ZZ",
            "cantidad": 1,
            "descripcion": "SERVICIO DE CONSULTORIA EN SISTEMAS",
            "valorUnitario": 5000.00,
            "precioUnitario": 5900.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        }
    ]
}'
```

---

### Ejemplo 4: Boleta sin documento (menor a S/700)

```bash
curl -X POST http://localhost:8080/api/comprobantes/emitir \
  -H "Content-Type: application/json" \
  -d '{
    "tipoComprobante": "03",
    "serie": "B001",
    "correlativo": 2,
    "tipoOperacion": "0101",
    "moneda": "PEN",
    "formaPago": "Contado",
    "clienteTipoDocumento": "0",
    "clienteNumeroDocumento": "00000000",
    "clienteRazonSocial": "CLIENTE VARIOS",
    "items": [
        {
            "numero": 1,
            "codigoProducto": "A001",
            "unidadMedida": "NIU",
            "cantidad": 3,
            "descripcion": "CABLE HDMI 2 METROS",
            "valorUnitario": 16.95,
            "precioUnitario": 20.00,
            "tipoPrecio": "01",
            "tipoAfectacionIgv": "10",
            "porcentajeIgv": 18.00,
            "codigoTributo": "1000"
        }
    ]
}'
```

---

### Catálogos principales SUNAT

#### Tipo de Comprobante (Catálogo 01)
| Código | Descripción |
|--------|-------------|
| 01 | Factura |
| 03 | Boleta de Venta |
| 07 | Nota de Crédito |
| 08 | Nota de Débito |

#### Tipo Documento Identidad (Catálogo 06)
| Código | Descripción |
|--------|-------------|
| 0 | Sin documento |
| 1 | DNI |
| 4 | Carnet de extranjería |
| 6 | RUC |
| 7 | Pasaporte |

#### Tipo Afectación IGV (Catálogo 07)
| Código | Descripción |
|--------|-------------|
| 10 | Gravado - Operación Onerosa |
| 20 | Exonerado - Operación Onerosa |
| 30 | Inafecto - Operación Onerosa |

#### Unidades de Medida
| Código | Descripción |
|--------|-------------|
| NIU | Unidad (pieza) |
| ZZ | Servicio |
| KGM | Kilogramo |
| LTR | Litro |
| MTR | Metro |
| GLL | Galón |

#### Tipo de Operación (Catálogo 51)
| Código | Descripción |
|--------|-------------|
| 0101 | Venta interna |
| 0112 | Venta interna - gastos deducibles |
| 0200 | Exportación de bienes |
| 0401 | Ventas no domiciliados |

---

### Estructura de Respuesta

```json
{
    "success": true,
    "message": "Comprobante emitido correctamente",
    "tipoComprobante": "01",
    "serieCorrelativo": "F001-00000001",
    "nombreArchivo": "20123456789-01-F001-00000001",
    "totalGravado": 25635.55,
    "totalExonerado": 0,
    "totalInafecto": 0,
    "totalIgv": 4614.40,
    "importeTotal": 30249.95,
    "sunatResponseCode": "0",
    "sunatDescription": "La Factura F001-00000001 ha sido aceptada",
    "sunatNote": "",
    "hashCode": "abc123...",
    "xmlBase64": "PD94bWw...",
    "cdrBase64": "UEsDBBQ..."
}
```

---

### Archivos generados

```
./generated-xml/    → XMLs UBL 2.1 firmados
./generated-zip/    → XMLs comprimidos para SUNAT
./received-cdr/     → CDRs (Constancias de Recepción)
```

---

### Configuración (application.yml)

Editar `src/main/resources/application.yml` con:
- **RUC y datos del emisor** de tu empresa
- **Credenciales SOL** (MODDATOS para pruebas)
- **Ruta del certificado** digital
- **Ambiente**: `beta` (pruebas) o `produccion`
