package com.facturacion.service;

import com.facturacion.config.SunatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Firma digitalmente el XML UBL 2.1 usando XMLDSig (enveloped signature).
 * Soporta múltiples empresas: carga y cachea certificados por RUC.
 */
@Service
public class SignatureService {

    private static final Logger log = LoggerFactory.getLogger(SignatureService.class);

    private final SunatConfig config;

    /** Caché de certificados por RUC */
    private final ConcurrentHashMap<String, CertificateHolder> certificateCache = new ConcurrentHashMap<>();

    public SignatureService(SunatConfig config) {
        this.config = config;
    }

    /**
     * Firma el documento XML con el certificado de la empresa identificada por RUC y ambiente
     */
    public Document sign(Document doc, String ruc, String ambiente) throws Exception {
        CertificateHolder holder = getCertificate(ruc, ambiente);

        XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");

        // Referencia al documento completo con transformación enveloped
        List<Transform> transforms = new ArrayList<>();
        transforms.add(signatureFactory.newTransform(
                Transform.ENVELOPED, (TransformParameterSpec) null));

        Reference reference = signatureFactory.newReference(
                "",
                signatureFactory.newDigestMethod(DigestMethod.SHA256, null),
                transforms,
                null,
                null
        );

        // Información firmada con Exclusive C14N (evita problemas de namespaces al serializar)
        SignedInfo signedInfo = signatureFactory.newSignedInfo(
                signatureFactory.newCanonicalizationMethod(
                        CanonicalizationMethod.EXCLUSIVE,
                        (C14NMethodParameterSpec) null),
                signatureFactory.newSignatureMethod(
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                Collections.singletonList(reference)
        );

        // KeyInfo con el certificado X509
        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(holder.certificate);
        X509Data x509Data = keyInfoFactory.newX509Data(x509Content);
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

        // Buscar el nodo ExtensionContent donde insertar la firma
        Node extensionContent = findExtensionContent(doc);
        if (extensionContent == null) {
            throw new IllegalStateException(
                "No se encontró el nodo ext:ExtensionContent en el XML");
        }

        // Contexto de firma
        DOMSignContext signContext = new DOMSignContext(holder.privateKey, extensionContent);
        signContext.setDefaultNamespacePrefix("ds");

        // Crear y ejecutar la firma
        XMLSignature xmlSignature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
        xmlSignature.sign(signContext);

        log.info("XML firmado digitalmente para RUC: {}", ruc);
        return doc;
    }

    /**
     * Obtiene el certificado del caché o lo carga del filesystem
     */
    private CertificateHolder getCertificate(String ruc, String ambiente) throws Exception {
        String cacheKey = ambiente + ":" + ruc;
        CertificateHolder cached = certificateCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Cargar desde filesystem
        String certPath = config.getCertificatePath(ruc, ambiente);
        String passwordPath = config.getCertificatePasswordPath(ruc, ambiente);

        File certFile = new File(certPath);
        if (!certFile.exists()) {
            throw new IllegalStateException(
                "No se encontró certificado para RUC " + ruc +
                ". Suba el certificado en: " + certPath);
        }

        // Leer contraseña
        String password;
        File passwordFile = new File(passwordPath);
        if (passwordFile.exists()) {
            password = Files.readString(Path.of(passwordPath)).trim();
        } else {
            throw new IllegalStateException(
                "No se encontró archivo de contraseña para RUC " + ruc +
                ". Cree el archivo: " + passwordPath);
        }

        // Cargar keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(certFile)) {
            keyStore.load(fis, password.toCharArray());
        }

        String alias = keyStore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

        CertificateHolder holder = new CertificateHolder(privateKey, certificate);
        certificateCache.put(cacheKey, holder);

        log.info("Certificado cargado y cacheado para RUC: {} desde: {}", ruc, certPath);
        return holder;
    }

    /**
     * Invalida el caché de un RUC (cuando se sube un nuevo certificado)
     */
    public void invalidateCache(String ruc, String ambiente) {
        certificateCache.remove(ambiente + ":" + ruc);
        log.info("Caché de certificado invalidado para RUC: {} ambiente: {}", ruc, ambiente);
    }

    private Node findExtensionContent(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2",
                "ExtensionContent"
        );
        return nodes.getLength() > 0 ? nodes.item(0) : null;
    }

    /** Holder inmutable para clave privada + certificado */
    private static class CertificateHolder {
        final PrivateKey privateKey;
        final X509Certificate certificate;

        CertificateHolder(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }
    }
}
