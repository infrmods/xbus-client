package com.xbus.client;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import com.xbus.exceptions.DeadlineExceededException;
import com.xbus.exceptions.XBusException;
import com.xbus.item.Config;
import com.xbus.item.Service;
import com.xbus.item.ServiceEndpoint;
import com.xbus.result.*;
import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusClient extends HttpClient implements ConfigClient, ServiceClient {
    private static final GsonFactory GSON_FACTORY = new GsonFactory();
    private XbusConfig config;

    private ConcurrentHashMap<String, Long> configRevisions = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> serviceRevisions = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> keepIds = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, String> addresses = new ConcurrentHashMap<String, String>();

    private String getConfigPath(String name) {
        return "/api/configs/" + name;
    }

    private String getServicePath(String name, String version) {
        return "/api/services/" + name + "/" + version;
    }

    private class XBusRequestInitializer implements HttpRequestInitializer {
        public void initialize(HttpRequest request) throws IOException {
            request.setParser(new JsonObjectParser(GSON_FACTORY));
            request.setReadTimeout(config.httpReadTimeout);
        }
    }

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
        HttpRequestFactory httpRequestFactory = httpTransport.createRequestFactory(new XBusRequestInitializer());
        initHttp(httpRequestFactory, config.endpoints);
    }

    public Config getConfig(String name) throws XBusException {
        GetConfigResult result = get(new Url(getConfigPath(name)), GetConfigResult.RESPONSE.class);
        configRevisions.putIfAbsent(name, result.revision);
        return result.config;
    }

    public void putConfig(String name, String value) throws XBusException {
        putConfig(name, value, null);
    }

    public void putConfig(String name, String value, Long currentVersion) throws XBusException {
        put(new Url(getConfigPath(name)),
                newUrlForm()
                        .put("value", value)
                        .putIfNotNull("version", currentVersion)
                        .build(),
                PutConfigResult.RESPONSE.class);
    }

    public Config watchConfig(String name) throws XBusException {
        return watchConfig(name, null);
    }

    public Config watchConfig(String name, String timeout) throws XBusException {
        WatchConfigResult result;
        try {
            result = get(new WatchUrl(getConfigPath(name), configRevisions.get(name), timeout),
                    WatchConfigResult.RESPONSE.class);
        } catch (DeadlineExceededException e) {
            return null;
        }
        configRevisions.put(name, result.revision);
        return result.config;
    }

    public Service getService(String name, String version) throws XBusException {
        GetServiceResult result = get(new Url(getServicePath(name, version)),
                GetServiceResult.RESPONSE.class);
        serviceRevisions.putIfAbsent(Service.genId(name, version), result.revision);
        result.service.name = name;
        result.service.version = version;
        return result.service;
    }


    public Service watchService(String name, String version) throws XBusException {
        return watchService(name, version, null);
    }

    public Service watchService(String name, String version, String timeout) throws XBusException {
        String id = Service.genId(name, version);
        Long revision = serviceRevisions.get(id);
        WatchServiceResult result;
        try {
            result = get(new WatchUrl(getServicePath(name, version), revision, timeout),
                    WatchServiceResult.RESPONSE.class);
        } catch (DeadlineExceededException e) {
            return null;
        }
        serviceRevisions.put(id, result.revision);
        return result.service;
    }

    public void plugService(Service service) throws XBusException {
        plugService(service, null);
    }

    public void plugService(Service service, Long ttl) throws XBusException {
        if (service.endpoints == null || service.endpoints.length != 1) {
            throw new RuntimeException("endpoints must be 1");
        }
        PlugServiceResult result = post(
                new Url(getServicePath(service.name, service.version)),
                newUrlForm()
                        .put("desc", service.getDesc())
                        .put("endpoint", service.endpoints[0])
                        .putIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        keepIds.put(service.getId(), result.keepId);
        addresses.put(service.getId(), service.endpoints[0].address);
    }

    public void unplugService(String name, String version) throws XBusException {
        String address = addresses.get(Service.genId(name, version));
        if (address == null) {
            throw new RuntimeException("missing address for " + Service.genId(name, version));
        }
        delete(new Url(getServicePath(name, version)), VoidResult.RESPONSE.class);
    }

    public void keepService(String name, String version) throws XBusException {
        String id = Service.genId(name, version);
        String address = addresses.get(id);
        if (address == null) {
            throw new RuntimeException("missing address for " + id);
        }
        Long keepId = keepIds.get(id);
        if (keepId == null) {
            throw new RuntimeException("missing keep id for " + id);
        }
        put(new Url(getServicePath(name, version)),
                newUrlForm().put("keep_id", keepId).build(),
                VoidResult.RESPONSE.class);
    }

    public void updateServiceConfig(String name, String version, String config) throws XBusException {
        String id = Service.genId(name, version);
        String address = addresses.get(id);
        if (address == null) {
            throw new RuntimeException("missing address for " + id);
        }
        Long keepId = keepIds.get(id);
        if (keepId == null) {
            throw new RuntimeException("missing keep id for " + id);
        }
        ServiceEndpoint endpoint = new ServiceEndpoint(null, config);
        put(new Url(getServicePath(name, version)),
                newUrlForm().put("endpoint", endpoint).build(),
                VoidResult.RESPONSE.class);
    }

    private class WatchUrl extends Url {
        @Key
        Long revision;

        @Key
        String timeout;

        @Key
        String watch = "true";

        WatchUrl(String path, Long currentRevision, String timeout) {
            super(path);
            if (currentRevision != null) {
                this.revision = currentRevision + 1;
            }
            this.timeout = timeout;
        }
    }
}