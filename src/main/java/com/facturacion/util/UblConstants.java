package com.facturacion.util;

/**
 * Constantes UBL 2.1 y catálogos SUNAT según normativa vigente
 * Referencia: Resolución de Superintendencia N° 340-2017/SUNAT y modificatorias
 */
public final class UblConstants {

    private UblConstants() {}

    // ==================== NAMESPACES UBL 2.1 ====================
    
    public static final String NS_INVOICE = 
        "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    public static final String NS_CREDIT_NOTE = 
        "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    public static final String NS_DEBIT_NOTE = 
        "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";
    public static final String NS_CAC = 
        "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    public static final String NS_CBC = 
        "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    public static final String NS_EXT = 
        "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    public static final String NS_DS = 
        "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_SAC = 
        "urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1";
    
    // Namespaces SUNAT para Bajas y Resúmenes
    public static final String NS_VOIDED = 
        "urn:sunat:names:specification:ubl:peru:schema:xsd:VoidedDocuments-1";
    public static final String NS_SUMMARY = 
        "urn:sunat:names:specification:ubl:peru:schema:xsd:SummaryDocuments-1";
    public static final String NS_DESPATCH = 
        "urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2";
    public static final String NS_RETENTION = 
        "urn:sunat:names:specification:ubl:peru:schema:xsd:Retention-1";
    public static final String NS_PERCEPTION = 
        "urn:sunat:names:specification:ubl:peru:schema:xsd:Perception-1";
    public static final String NS_CCTS = 
        "urn:un:unece:uncefact:documentation:2";
    public static final String NS_QDT = 
        "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2";
    public static final String NS_UDT = 
        "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2";

    // ==================== VERSIONES ====================
    
    public static final String UBL_VERSION = "2.1";
    public static final String CUSTOMIZATION_ID = "2.0";

    // ==================== CATÁLOGO N° 01: TIPO DE DOCUMENTO ====================
    
    public static final String TIPO_FACTURA = "01";
    public static final String TIPO_BOLETA = "03";
    public static final String TIPO_NOTA_CREDITO = "07";
    public static final String TIPO_NOTA_DEBITO = "08";

    // ==================== CATÁLOGO N° 05: TIPO DE TRIBUTO ====================
    
    public static final String TRIBUTO_IGV_ID = "1000";
    public static final String TRIBUTO_IGV_NOMBRE = "IGV";
    public static final String TRIBUTO_IGV_CODIGO = "VAT";

    public static final String TRIBUTO_ISC_ID = "2000";
    public static final String TRIBUTO_ISC_NOMBRE = "ISC";
    public static final String TRIBUTO_ISC_CODIGO = "EXC";

    public static final String TRIBUTO_EXONERADO_ID = "9997";
    public static final String TRIBUTO_EXONERADO_NOMBRE = "EXO";
    public static final String TRIBUTO_EXONERADO_CODIGO = "VAT";

    public static final String TRIBUTO_INAFECTO_ID = "9998";
    public static final String TRIBUTO_INAFECTO_NOMBRE = "INA";
    public static final String TRIBUTO_INAFECTO_CODIGO = "FRE";

    public static final String TRIBUTO_GRATUITO_ID = "9996";
    public static final String TRIBUTO_GRATUITO_NOMBRE = "GRA";
    public static final String TRIBUTO_GRATUITO_CODIGO = "FRE";

    // ==================== CATÁLOGO N° 05 - TaxCategory ID (UN/ECE 5305) ====================

    /** S = Gravado (Standard rate) - Operación Onerosa */
    public static final String TAX_CATEGORY_GRAVADO = "S";
    /** E = Exonerado (Exempt from tax) */
    public static final String TAX_CATEGORY_EXONERADO = "E";
    /** O = Inafecto (Not subject to tax / Free of tax) */
    public static final String TAX_CATEGORY_INAFECTO = "O";
    /** Z = Exportación (Zero rated goods) */
    public static final String TAX_CATEGORY_EXPORTACION = "Z";
    /** G = Gratuito (Free export item, tax not charged) */
    public static final String TAX_CATEGORY_GRATUITO = "Z";

    // ==================== CATÁLOGO N° 06: TIPO DOC IDENTIDAD ====================
    
    public static final String DOC_RUC = "6";
    public static final String DOC_DNI = "1";
    public static final String DOC_SIN_DOCUMENTO = "0";
    public static final String DOC_CARNET_EXTRANJERIA = "4";
    public static final String DOC_PASAPORTE = "7";

    // ==================== CATÁLOGO N° 07: TIPO AFECTACIÓN IGV ====================
    
    public static final String AFECTACION_GRAVADO = "10";
    public static final String AFECTACION_EXONERADO = "20";
    public static final String AFECTACION_INAFECTO = "30";
    public static final String AFECTACION_GRATUITO = "40";

    // Subtipos gravados (Catálogo 07 completo)
    public static final String AFECTACION_GRAVADO_ONEROSA = "10";
    public static final String AFECTACION_GRAVADO_RETIRO_PREMIO = "11";
    public static final String AFECTACION_GRAVADO_RETIRO_DONACION = "12";
    public static final String AFECTACION_GRAVADO_RETIRO = "13";
    public static final String AFECTACION_GRAVADO_RETIRO_PUBLICIDAD = "14";
    public static final String AFECTACION_GRAVADO_BONIFICACIONES = "15";
    public static final String AFECTACION_GRAVADO_RETIRO_ENTREGA_TRABAJADORES = "16";
    public static final String AFECTACION_GRAVADO_IVAP = "17";

    // Subtipos exonerados
    public static final String AFECTACION_EXONERADO_ONEROSA = "20";
    public static final String AFECTACION_EXONERADO_GRATUITA = "21";

    // Subtipos inafectos
    public static final String AFECTACION_INAFECTO_ONEROSA = "30";
    public static final String AFECTACION_INAFECTO_RETIRO_PREMIO = "31";
    public static final String AFECTACION_INAFECTO_RETIRO_DONACION = "32";
    public static final String AFECTACION_INAFECTO_RETIRO = "33";
    public static final String AFECTACION_INAFECTO_RETIRO_PUBLICIDAD = "34";
    public static final String AFECTACION_INAFECTO_BONIFICACIONES = "35";
    public static final String AFECTACION_INAFECTO_RETIRO_ENTREGA_TRABAJADORES = "36";
    public static final String AFECTACION_EXPORTACION = "40";

    // ==================== CATÁLOGO N° 03: UNIDAD DE MEDIDA (UN/ECE Rec 20) ====================

    public static final String UM_UNIDAD = "NIU";       // Unidad (bienes)
    public static final String UM_KILOGRAMO = "KGM";     // Kilogramo
    public static final String UM_LITRO = "LTR";         // Litro
    public static final String UM_METRO = "MTR";         // Metro
    public static final String UM_METRO_CUADRADO = "MTK"; // Metro cuadrado
    public static final String UM_METRO_CUBICO = "MTQ";  // Metro cúbico
    public static final String UM_TONELADA = "TNE";      // Tonelada métrica
    public static final String UM_GALONES = "GLL";       // Galón
    public static final String UM_DOCENA = "DZN";        // Docena
    public static final String UM_PAQUETE = "PK";        // Paquete
    public static final String UM_CAJA = "BX";           // Caja
    public static final String UM_MILLAR = "MIL";        // Millar
    public static final String UM_SERVICIO = "ZZ";       // Servicio (no definido)
    public static final String UM_PIEZA = "EA";          // Pieza/Each
    public static final String UM_GRAMO = "GRM";         // Gramo
    public static final String UM_HORA = "HUR";          // Hora
    public static final String UM_DIA = "DAY";           // Día
    public static final String UM_ROLLO = "RO";          // Rollo
    public static final String UM_JUEGO = "SET";         // Juego/Set
    public static final String UM_PAR = "PR";            // Par
    public static final String UM_BOLSA = "BG";          // Bolsa
    public static final String UM_BOBINA = "BO";         // Bobinas
    public static final String UM_LATA = "CA";           // Lata
    public static final String UM_BARRIL = "BLL";        // Barril

    /** Unidades de medida válidas según SUNAT (UN/ECE Rec 20 aceptadas) */
    public static final java.util.Set<String> UNIDADES_MEDIDA_VALIDAS = java.util.Set.of(
            "NIU", "KGM", "LTR", "MTR", "MTK", "MTQ", "TNE", "GLL", "DZN",
            "PK", "BX", "MIL", "ZZ", "EA", "GRM", "HUR", "DAY", "RO", "SET",
            "PR", "BG", "BO", "CA", "BLL", "FOT", "INH", "LBR", "ONZ",
            "YRD", "CMT", "MLT", "KWH", "MON", "XBX", "XUN", "XPK", "XCI",
            "XBG", "XSA", "XBA", "XBE", "XRO", "XST", "XPA", "U"
    );

    /** Tipos de documento de identidad válidos según SUNAT Catálogo 06 */
    public static final java.util.Set<String> TIPOS_DOCUMENTO_VALIDOS = java.util.Set.of(
            "0", // Sin documento
            "1", // DNI
            "4", // Carnet de extranjería
            "6", // RUC
            "7", // Pasaporte
            "A", // Cédula diplomática
            "B", // Doc. identidad país residencia - No domiciliado
            "C", // Tax Identification Number - TIN
            "D", // Identification Number - IN
            "E"  // TAM - Tarjeta Andina de Migración
    );

    // ==================== CATÁLOGO N° 51: TIPO OPERACIÓN ====================

    public static final String OPERACION_VENTA_INTERNA = "0101";
    public static final String OPERACION_EXPORTACION = "0200";
    public static final String OPERACION_NO_DOMICILIADOS = "0401";
    public static final String OPERACION_VENTA_INTERNA_ANTICIPO = "0112";
    public static final String OPERACION_VENTA_INTERNA_ITINERANTE = "0113";
    public static final String OPERACION_VENTA_ARROZ_PILADO = "0200";
    public static final String OPERACION_DETRACCIONES_RECURSOS_HIDRO = "1001";
    public static final String OPERACION_DETRACCIONES_SERV_TRANSPORTE = "1002";
    public static final String OPERACION_DETRACCIONES_OTROS = "1004";

    // ==================== CATÁLOGO N° 16: TIPO PRECIO ====================

    /** 01 = Precio unitario (incluye IGV) */
    public static final String PRECIO_UNITARIO = "01";
    /** 02 = Valor referencial unitario en operaciones no onerosas */
    public static final String PRECIO_REFERENCIAL = "02";

    // ==================== CATÁLOGO URIs SUNAT ====================

    public static final String CAT01_URI = "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01";
    public static final String CAT06_URI = "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06";
    public static final String CAT07_URI = "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo07";
    public static final String CAT16_URI = "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo16";

    // ==================== SCHEMA AGENCY ====================

    public static final String AGENCY_NAME = "PE:SUNAT";
    public static final String SCHEME_URI = "urn:pe:gob:sunat:cpe:see:gem:catalogos";

    // UN/ECE Attributes para TaxScheme
    public static final String UNECE_5153 = "UN/ECE 5153";
    public static final String UNECE_5305 = "UN/ECE 5305";

    // ==================== HELPERS ====================

    /**
     * Obtiene el TaxCategory ID según UN/ECE 5305 para el tipo de afectación IGV
     */
    public static String getTaxCategoryId(String tipoAfectacionIgv) {
        if (tipoAfectacionIgv == null) return TAX_CATEGORY_GRAVADO;
        switch (tipoAfectacionIgv.substring(0, 1)) {
            case "1": return TAX_CATEGORY_GRAVADO;    // 10-17 → S
            case "2": return TAX_CATEGORY_EXONERADO;  // 20-21 → E
            case "3": return TAX_CATEGORY_INAFECTO;   // 30-36 → O
            case "4": return TAX_CATEGORY_EXPORTACION; // 40 → Z (exportación)
            default:  return TAX_CATEGORY_GRAVADO;
        }
    }

    /**
     * Determina si la operación es gratuita según el tipo de afectación
     */
    public static boolean esOperacionGratuita(String tipoAfectacionIgv) {
        if (tipoAfectacionIgv == null) return false;
        int codigo = Integer.parseInt(tipoAfectacionIgv);
        // 11-17 (gravados gratuitos), 21 (exonerado gratuito), 31-36 (inafectos gratuitos)
        return (codigo >= 11 && codigo <= 17) || codigo == 21 || (codigo >= 31 && codigo <= 36);
    }
}
