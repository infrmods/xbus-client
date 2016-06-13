package com.xbus.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Created by lolynx on 6/11/16.
 */
public class XbusConfig {
    public String[] endpoints;
    public String keystoreType = "JKS";
    public String keystorePath;
    public String keystorePassword;

    public XbusConfig() {
    }

    public XbusConfig(String[] endpoints, String keystorePath, String keystorePassword) {
        this.endpoints = endpoints;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public KeyStore loadKeyStore() throws KeyStoreLoadException {
        KeyStore keyStore;
        FileInputStream ksStream;
        try {
            keyStore = KeyStore.getInstance(keystoreType);
            ksStream = new FileInputStream(keystorePath);
        } catch (KeyStoreException e) {
            throw new KeyStoreLoadException(e);
        } catch (FileNotFoundException e) {
            throw new KeyStoreLoadException(e);
        }

        try {
            keyStore.load(ksStream, keystorePassword.toCharArray());
        } catch (CertificateException e) {
            throw new KeyStoreLoadException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreLoadException(e);
        } catch (IOException e) {
            throw new KeyStoreLoadException(e);
        } finally {
            try {
                ksStream.close();
            } catch (IOException e) {
                throw new KeyStoreLoadException(e);
            }
        }
        return keyStore;
    }
}
