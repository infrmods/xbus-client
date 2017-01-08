package com.github.infrmods.xbus.client;

import java.security.KeyStore;
import java.util.Random;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusConfig {
    private Random random = new Random();
    public static class Endpoint {
        public String host;
        public Integer port;

        public Endpoint(String s) {
            if (s.startsWith("https://")) {
                s = s.substring("https://".length());
            }
            String[] parts = s.split(":", 2);
            host = parts[0];
            if (parts.length == 2) {
                port = Integer.valueOf(parts[1]);
            }
        }

        public static Endpoint[] convert(String endpoints[]) {
            Endpoint[] result = new Endpoint[endpoints.length];
            for (int i = 0; i< endpoints.length; i++) {
                result[i] = new Endpoint(endpoints[i]);
            }
            return result;
        }

        public Endpoint(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
    }

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
    }

    public XBusConfig(Endpoint[] endpoints, KeyStore keyStore, String keystorePassword) {
        this.endpoints = endpoints;
        this.keyStore = keyStore;
        this.keystorePassword = keystorePassword;
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
}
