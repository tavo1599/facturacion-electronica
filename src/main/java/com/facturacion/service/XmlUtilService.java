package com.facturacion.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utilidades para manejo de XML, ZIP y hashes
 */
@Service
public class XmlUtilService {

    /**
     * Convierte un Document XML a array de bytes.
     * IMPORTANTE: 
     * - NO usa indentación para preservar la integridad de la firma digital.
     * - Elimina entidades &#13; (CR) que el Transformer de Java inserta al serializar
     *   los nodos de texto del XMLDSig (SignatureValue, X509Certificate, DigestValue).
     *   Estos &#13; corrompen la verificación RSA en SUNAT causando:
     *   "El documento electrónico ingresado ha sido alterado - RSA signature did not verify"
     */
    public byte[] documentToBytes(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        // Eliminar &#13; que Java Transformer añade al escapar \r en nodos de texto.
        // Esto es crítico: sin esta limpieza, SUNAT rechaza la firma con error 2335.
        String xml = baos.toString("UTF-8");
        xml = xml.replace("&#13;", "");
        return xml.getBytes("UTF-8");
    }

    /**
     * Convierte un Document XML a bytes CON indentación (solo para visualización/debug, 
     * NO usar para XML firmado que se enviará a SUNAT)
     */
    public byte[] documentToBytesIndented(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        return baos.toByteArray();
    }

    /**
     * Convierte un Document XML a String
     */
    public String documentToString(Document doc) throws Exception {
        return new String(documentToBytes(doc), "UTF-8");
    }

    /**
     * Convierte bytes a Base64
     */
    public String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Comprime el XML en formato ZIP (requerido por SUNAT)
     * @param fileName nombre del archivo XML dentro del ZIP
     * @param xmlBytes contenido del XML
     * @return bytes del archivo ZIP
     */
    public byte[] zipXml(String fileName, byte[] xmlBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(xmlBytes);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * Descomprime un ZIP y retorna el contenido del primer archivo
     */
    public byte[] unzip(byte[] zipBytes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    return zis.readAllBytes();
                }
            }
        }
        return new byte[0];
    }

    /**
     * Genera el hash SHA-256 del XML (para el código de barras/QR)
     */
    public String generateHash(byte[] xmlBytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(xmlBytes);
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Guarda bytes en un archivo
     */
    public void saveToFile(byte[] data, String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
}
