package com.github.infrmods.xbus.client;

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
    public static class Endpoint {
        public String host;
        public Integer port;

        public Endpoint(String s) {
            String[] parts = s.split(":", 2);
            host = parts[0];
            if (parts.length == 2) {
                port = Integer.valueOf(parts[1]);
            }
        }

        public Endpoint(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
    }

    public Endpoint[] endpoints;
    public String keystoreType = "JKS";
    public String keystorePath;
    public String keystorePassword;
    public int httpReadTimeout = 5 * 60;

    public XbusConfig() {
    }

    public XbusConfig(String[] endpoints, String keystorePath, String keystorePassword) {
        this.endpoints = new Endpoint[endpoints.length];
        for (int i = 0; i< endpoints.length; i++) {
            this.endpoints[i] = new Endpoint(endpoints[i]);
        }
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public XbusConfig(Endpoint[] endpoints, String keystorePath, String keystorePassword) {
        this.endpoints = endpoints;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public KeyStore loadKeyStore() throws TLSInitException {
        KeyStore keyStore;
        FileInputStream ksStream;
        try {
            keyStore = KeyStore.getInstance(keystoreType);
            ksStream = new FileInputStream(keystorePath);
        } catch (KeyStoreException e) {
            throw new TLSInitException(e);
        } catch (FileNotFoundException e) {
            throw new TLSInitException(e);
        }

        try {
            keyStore.load(ksStream, keystorePassword.toCharArray());
        } catch (CertificateException e) {
            throw new TLSInitException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new TLSInitException(e);
        } catch (IOException e) {
            throw new TLSInitException(e);
        } finally {
            try {
                ksStream.close();
            } catch (IOException e) {
                throw new TLSInitException(e);
            }
        }
        return keyStore;
    }
}
