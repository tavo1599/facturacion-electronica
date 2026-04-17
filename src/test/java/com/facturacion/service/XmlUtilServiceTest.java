package com.facturacion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para XmlUtilService - serialización, ZIP, hash, Base64
 */
class XmlUtilServiceTest {

    private XmlUtilService service;

    @BeforeEach
    void setUp() {
        service = new XmlUtilService();
    }

    @Test
    @DisplayName("documentToBytes genera bytes válidos")
    void documentToBytes() throws Exception {
        Document doc = createSimpleDoc();
        byte[] bytes = service.documentToBytes(doc);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        String xml = new String(bytes, "UTF-8");
        assertTrue(xml.contains("test"));
    }

    @Test
    @DisplayName("documentToString genera string válido")
    void documentToString() throws Exception {
        Document doc = createSimpleDoc();
        String xml = service.documentToString(doc);
        assertNotNull(xml);
        assertTrue(xml.contains("test"));
    }

    @Test
    @DisplayName("toBase64 codifica correctamente")
    void toBase64() {
        byte[] data = "Hola SUNAT".getBytes();
        String b64 = service.toBase64(data);
        assertNotNull(b64);
        assertEquals("SG9sYSBTVU5BVA==", b64);
    }

    @Test
    @DisplayName("zipXml y unzip roundtrip")
    void zipUnzipRoundtrip() throws Exception {
        byte[] original = "<Invoice>test</Invoice>".getBytes();
        byte[] zipped = service.zipXml("test.xml", original);
        assertNotNull(zipped);
        assertTrue(zipped.length > 0);

        byte[] unzipped = service.unzip(zipped);
        assertArrayEquals(original, unzipped);
    }

    @Test
    @DisplayName("generateHash produce hash SHA-256 en Base64")
    void generateHash() throws Exception {
        byte[] data = "test data".getBytes();
        String hash = service.generateHash(data);
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        // SHA-256 en Base64 siempre tiene 44 caracteres
        assertEquals(44, hash.length());
    }

    @Test
    @DisplayName("generateHash es determinístico")
    void hashDeterministico() throws Exception {
        byte[] data = "facturacion electronica".getBytes();
        String hash1 = service.generateHash(data);
        String hash2 = service.generateHash(data);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("saveToFile guarda archivo correctamente")
    void saveToFile() throws Exception {
        Path tmpDir = Files.createTempDirectory("facturacion-test");
        String filePath = tmpDir.toString() + "/test-output.xml";
        byte[] data = "<Invoice>test</Invoice>".getBytes();

        service.saveToFile(data, filePath);

        File file = new File(filePath);
        assertTrue(file.exists());
        assertEquals(data.length, file.length());

        // Cleanup
        file.delete();
        tmpDir.toFile().delete();
    }

    @Test
    @DisplayName("saveToFile crea directorios padre")
    void saveToFileCreaDirs() throws Exception {
        Path tmpDir = Files.createTempDirectory("facturacion-test");
        String filePath = tmpDir.toString() + "/subdir1/subdir2/test.xml";
        byte[] data = "data".getBytes();

        service.saveToFile(data, filePath);

        assertTrue(new File(filePath).exists());

        // Cleanup
        new File(filePath).delete();
        new File(tmpDir.toString() + "/subdir1/subdir2").delete();
        new File(tmpDir.toString() + "/subdir1").delete();
        tmpDir.toFile().delete();
    }

    @Test
    @DisplayName("unzip retorna vacío para ZIP sin entries")
    void unzipVacio() throws Exception {
        // ZIP vacío (solo header)
        byte[] emptyZip = service.zipXml("empty.xml", new byte[0]);
        byte[] result = service.unzip(emptyZip);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // ==================== HELPER ====================

    private Document createSimpleDoc() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.appendChild(doc.createElement("test"));
        return doc;
    }
}

