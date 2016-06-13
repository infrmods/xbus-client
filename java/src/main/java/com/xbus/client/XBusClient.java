package com.xbus.client;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import org.apache.http.conn.ssl.SSLSocketFactory;

import java.security.KeyStore;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusClient extends ConfigClient {
    XbusConfig config;

    public XBusClient(XbusConfig config) throws KeyStoreLoadException {
        this.config = config;
        KeyStore keyStore = config.loadKeyStore();
        SSLSocketFactory factory;
        try {
            factory = new SSLSocketFactory(keyStore, config.keystorePassword, keyStore);
        } catch (Exception e) {
            throw new KeyStoreLoadException(e);
        }

        HttpTransport httpTransport = new ApacheHttpTransport.Builder().setSocketFactory(factory).build();
        initHttp(httpTransport, config.endpoints);
    }
}