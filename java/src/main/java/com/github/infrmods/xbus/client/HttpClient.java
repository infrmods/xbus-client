package com.github.infrmods.xbus.client;

import com.google.gson.Gson;
import com.github.infrmods.xbus.exceptions.ErrorCode;
import com.github.infrmods.xbus.exceptions.XBusException;
import com.github.infrmods.xbus.result.Response;
import com.github.infrmods.xbus.result.Result;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by lolynx on 6/18/16.
 */
public class HttpClient {
    final Gson gson = new Gson();
    private OkHttpClient client;
    private XBusConfig config;

    HttpClient(XBusConfig config) throws TLSInitException {
        this.config = config;
        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers;
        X509TrustManager trustManager;
        SSLContext sslContext;

        KeyStore keyStore = config.getKeyStore();
        try {
            if (keyStore == null) {
                trustManager = trustAllManager;
                trustManagers = new TrustManager[]{trustAllManager};
            } else {
                char[] passwd = config.getKeystorePassword().toCharArray();
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (keyStore.getKey(alias, passwd) != null) {
                        if (config.getAppName() != null) {
                            throw new TLSInitException("appName | keyCert duplicated");
                        }
                        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmFactory.init(keyStore, passwd);
                        keyManagers = kmFactory.getKeyManagers();
                        break;
                    }
                }

                TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmFactory.init(keyStore);
                trustManagers = tmFactory.getTrustManagers();
                trustManager = null;
                for (TrustManager manager : trustManagers) {
                    if (manager instanceof X509TrustManager) {
                        trustManager = (X509TrustManager) manager;
                    }
                }
                if (trustManager == null) {
                    throw new TLSInitException("missing trust manager");
                }
            }
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        } catch (Exception e) {
            throw new TLSInitException(e);
        }

        client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                .connectionPool(new ConnectionPool())
                .readTimeout(config.getHttpReadTimeout(), TimeUnit.SECONDS)
                .build();
    }

    <T extends Result> T get(HttpUrl url, Class<? extends Response<T>> cls) throws XBusException {
        Request request = requestBuilder().get().url(url).build();
        return getResult(request, cls);
    }

    <T extends Result> T delete(HttpUrl url, Class<? extends Response<T>> cls) throws XBusException {
        Request request = requestBuilder().delete().url(url).build();
        return getResult(request, cls);
    }

    <T extends Result> T post(HttpUrl url, RequestBody requestBody, Class<? extends Response<T>> cls) throws XBusException {
        if (requestBody == null) {
            requestBody = RequestBody.create(null, new byte[0]);
        }
        Request request = requestBuilder().post(requestBody).url(url).build();
        return getResult(request, cls);
    }

    <T extends Result> T put(HttpUrl url, RequestBody requestBody, Class<? extends Response<T>> cls) throws XBusException {
        Request request = requestBuilder().put(requestBody).url(url).build();
        return getResult(request, cls);
    }

    private Request.Builder requestBuilder() {
        Request.Builder builder = new Request.Builder();
        if (config.getAppName() != null) {
            builder.addHeader("Dev-App", config.getAppName());
        }
        return builder;
    }

    private <T extends Result> T getResult(Request request, Class<? extends Response<T>> cls) throws XBusException {
        String bodyContent;
        try {
            bodyContent = client.newCall(request).execute().body().string();
        } catch (IOException e) {
            throw new XBusException(ErrorCode.IOError, e);
        }
        Response<T> response = gson.fromJson(bodyContent, cls);
        return response.getResult();
    }

    class UrlBuilder {
        HttpUrl.Builder builder;

        UrlBuilder(String path) {
            Endpoint endpoint = config.chooseEndpoint();
            builder = new HttpUrl.Builder().scheme("https").host(endpoint.host).encodedPath(path);
            if (endpoint.port != null) {
                builder.port(endpoint.port);
            }
        }

        UrlBuilder param(String name, String value) {
            builder.addQueryParameter(name, value);
            return this;
        }

        HttpUrl url() {
            return builder.build();
        }
    }

    class WatchUrlBuilder extends UrlBuilder {
        WatchUrlBuilder(String path, Long currentRevision, Integer timeout) {
            super(path);
            builder.addQueryParameter("watch", "true");
            if (currentRevision != null) {
                builder.addQueryParameter("revision", String.valueOf(currentRevision + 1));
            }
            if (timeout != null) {
                builder.addQueryParameter("timeout", timeout.toString());
            }
        }
    }

    class FormBuilder {
        FormBody.Builder builder;

        FormBuilder() {
            builder = new FormBody.Builder();
        }

        FormBuilder add(String name, Object value) {
            builder.add(name, value.toString());
            return this;
        }

        FormBuilder addIfNotNull(String name, Object value) {
            if (value != null) {
                builder.add(name, value.toString());
            }
            return this;
        }

        FormBody build() {
            return builder.build();
        }
    }

    final X509TrustManager trustAllManager = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
