package com.github.infrmods.xbus.client;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Random;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusConfig {
    private Random random = new Random();

    private String appName;
    private boolean allowInsecure = false;
    private Endpoint[] endpoints;
    private KeyStore keyStore;
    private String keystorePassword;
    private int httpReadTimeout = 5 * 60;

    public XBusConfig(String[] endpoints, String keystorePath, String keystorePassword) {
        this(Endpoint.convert(endpoints), keystorePath, keystorePassword);
    }

    public XBusConfig(Endpoint[] endpoints, String keystorePath, String keystorePassword) {
        this(endpoints, KeyTool.loadKeyStore(keystorePath, keystorePassword), keystorePassword);
    }

    public XBusConfig(String[] endpoints, String cacertFile, String certFile, String keyFile) {
        this(Endpoint.convert(endpoints), cacertFile, certFile, keyFile);
    }

    public XBusConfig(Endpoint[] endpoints, String cacertFile, String certFile, String keyFile) {
        this(endpoints, KeyTool.keyStoreFromPem(cacertFile, certFile, keyFile, ""), "");
        this.keystorePassword = "";
    }

    public XBusConfig(Endpoint[] endpoints, KeyStore keyStore, String keystorePassword) {
        this.endpoints = endpoints;
        this.keyStore = keyStore;
        this.keystorePassword = keystorePassword;
    }

    public XBusConfig(String[] endpoints, String appName) {
        this(Endpoint.convert(endpoints), appName);
    }

    public XBusConfig(Endpoint[] endpoints, String appName) {
        this.endpoints = endpoints;
        this.appName = appName;
    }

    public Endpoint chooseEndpoint() {
        return endpoints[random.nextInt(endpoints.length)];
    }

    public void setHttpReadTimeout(int httpReadTimeout) {
        this.httpReadTimeout = httpReadTimeout;
    }

    public int getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isAllowInsecure() {
        return allowInsecure;
    }

    public void setAllowInsecure(boolean allowInsecure) {
        this.allowInsecure = allowInsecure;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }
}
