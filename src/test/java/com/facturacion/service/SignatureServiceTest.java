package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para SignatureService - firma digital XML
 * Solo prueba la lógica de búsqueda de ExtensionContent y manejo de errores
 * (la firma real requiere certificado)
 */
class SignatureServiceTest {

    @Test
    @DisplayName("sign lanza error si no hay certificado para el RUC")
    void sinCertificado() throws Exception {
        SunatConfig config = new SunatConfig();
        config.setCertificatesBasePath("/tmp/nonexistent-certs");

        SignatureService svc = new SignatureService(config);

        Document doc = createDocWithExtension();

        assertThrows(IllegalStateException.class, () -> svc.sign(doc, "20123456789", "beta"));
    }

    @Test
    @DisplayName("sign lanza error si no encuentra ExtensionContent")
    void sinExtensionContent() throws Exception {
        SunatConfig config = new SunatConfig();
        config.setCertificatesBasePath("/tmp/nonexistent-certs");

        SignatureService svc = new SignatureService(config);
        Document doc = createSimpleDoc();

        assertThrows(IllegalStateException.class, () -> svc.sign(doc, "20123456789", "beta"));
    }

    private Document createDocWithExtension() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        String nsExt = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
        var root = doc.createElementNS("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2", "Invoice");
        var extensions = doc.createElementNS(nsExt, "ext:UBLExtensions");
        var extension = doc.createElementNS(nsExt, "ext:UBLExtension");
        var content = doc.createElementNS(nsExt, "ext:ExtensionContent");
        extension.appendChild(content);
        extensions.appendChild(extension);
        root.appendChild(extensions);
        doc.appendChild(root);
        return doc;
    }

    private Document createSimpleDoc() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.appendChild(doc.createElement("test"));
        return doc;
    }
}
