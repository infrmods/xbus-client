package com.github.infrmods.xbus.client;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.security.*;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Created by lo on 1/8/17.
 */
public class KeyTool {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static KeyStore loadKeyStore(String filename, String password) throws TLSInitException {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            FileInputStream in = new FileInputStream(filename);
            try {
                keyStore.load(in, password.toCharArray());
                return keyStore;
            } finally {
                in.close();
            }
        } catch (FileNotFoundException e) {
            throw new TLSInitException(e);
        } catch (CertificateException e) {
            throw new TLSInitException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new TLSInitException(e);
        } catch (KeyStoreException e) {
            throw new TLSInitException(e);
        } catch (IOException e) {
            throw new TLSInitException(e);
        }
    }

    public static KeyStore keyStoreFromPem(String cacertFile, String certFile,
                                           String keyFile, String password) throws TLSInitException {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            if (cacertFile != null) {
                Certificate cacert = KeyTool.loadCert(cacertFile);
                keyStore.setCertificateEntry("cacert", cacert);
            }
            if (certFile != null && keyFile != null) {
                PrivateKey privateKey = KeyTool.loadPrivateKey(keyFile);
                Certificate cert = KeyTool.loadCert(certFile);
                keyStore.setKeyEntry("1", privateKey, password.toCharArray(), new Certificate[]{cert});
            }
            return keyStore;
        } catch (IOException e) {
            throw new TLSInitException(e);
        } catch (CertificateException e) {
            throw new TLSInitException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new TLSInitException(e);
        } catch (KeyStoreException e) {
            throw new TLSInitException(e);
        } catch (NoSuchProviderException e) {
            throw new TLSInitException(e);
        } catch (InvalidKeySpecException e) {
            throw new TLSInitException(e);
        }
    }

    public static PrivateKey loadPrivateKey(String filename) throws IOException, InvalidKeySpecException, NoSuchProviderException, NoSuchAlgorithmException {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        FileInputStream in = new FileInputStream(filename);
        try {
            PemReader reader = new PemReader(new InputStreamReader(in));
            PemObject pem = reader.readPemObject();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pem.getContent());
            return factory.generatePrivate(privKeySpec);
        } finally {
            in.close();
        }
    }

    public static Certificate loadCert(String filename) throws IOException, CertificateException {
        FileInputStream in = new FileInputStream(filename);
        try {
            PemReader reader = new PemReader(new InputStreamReader(in));
            PemObject pem = reader.readPemObject();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(new ByteArrayInputStream(pem.getContent()));
        } finally {
            in.close();
        }
    }
}
