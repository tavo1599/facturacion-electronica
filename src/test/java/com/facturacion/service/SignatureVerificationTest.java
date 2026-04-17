package com.facturacion.service;

import com.facturacion.util.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.*;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el flujo firma → serialización produce un XML con firma válida
 */
@SpringBootTest
class SignatureVerificationTest {

    @Autowired
    private XmlBuilderService xmlBuilderService;

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private XmlUtilService xmlUtilService;

    @Test
    void firmaDebeSerValidaDespuesDeSerializar() throws Exception {
        // 1. Generar XML con datos de prueba
        Document xmlDoc = xmlBuilderService.buildXml(TestDataBuilder.facturaGravada());

        // 2. Firmar (usando el RUC de la empresa de prueba)
        String ruc = TestDataBuilder.facturaGravada().getEmpresa().getRuc();
        Document signedDoc = signatureService.sign(xmlDoc, ruc, "beta");

        // 3. Serializar (como se haría antes de enviar a SUNAT)
        byte[] xmlBytes = xmlUtilService.documentToBytes(signedDoc);

        // 4. Re-parsear el XML serializado
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document reparsed = dbf.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xmlBytes));

        // 5. Verificar la firma
        NodeList signatureNodes = reparsed.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertEquals(1, signatureNodes.getLength(), "Debe haber exactamente una firma");

        DOMValidateContext valContext = new DOMValidateContext(new KeySelector() {
            public KeySelectorResult select(KeyInfo ki, KeySelector.Purpose p, AlgorithmMethod m, XMLCryptoContext c) throws KeySelectorException {
                for (Object o : ki.getContent()) {
                    if (o instanceof X509Data) {
                        for (Object x : ((X509Data) o).getContent()) {
                            if (x instanceof X509Certificate) {
                                final PublicKey key = ((X509Certificate) x).getPublicKey();
                                return () -> key;
                            }
                        }
                    }
                }
                throw new KeySelectorException("No key found");
            }
        }, signatureNodes.item(0));

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        XMLSignature sig = fac.unmarshalXMLSignature(valContext);

        boolean valid = sig.validate(valContext);

        if (!valid) {
            boolean svValid = sig.getSignatureValue().validate(valContext);
            System.out.println("SignatureValue valid: " + svValid);
            for (Object r : sig.getSignedInfo().getReferences()) {
                boolean refValid = ((Reference) r).validate(valContext);
                System.out.println("Reference URI='" + ((Reference) r).getURI() + "' valid: " + refValid);
            }
        }

        assertTrue(valid, "La firma digital debe ser válida después de serializar con documentToBytes");
    }
}
