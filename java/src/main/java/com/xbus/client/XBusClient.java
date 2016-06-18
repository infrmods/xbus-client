package com.xbus.client;

import com.xbus.exceptions.DeadlineExceededException;
import com.xbus.exceptions.ErrorCode;
import com.xbus.exceptions.XBusException;
import com.xbus.item.Config;
import com.xbus.item.Service;
import com.xbus.item.ServiceDesc;
import com.xbus.item.ServiceEndpoint;
import com.xbus.result.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusClient extends HttpClient implements ConfigClient, ServiceClient {
    private ConcurrentHashMap<String, Long> configRevisions = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> serviceRevisions = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> leaseIds = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, String> addresses = new ConcurrentHashMap<String, String>();

    private String getConfigPath(String name) {
        return "/api/configs/" + name;
    }

    private String getServicePath(String name, String version) {
        return "/api/services/" + name + "/" + version;
    }

    public XBusClient(XbusConfig config) throws TLSInitException {
        super(config);
    }

    public Config getConfig(String name) throws XBusException {
        GetConfigResult result = get(new UrlBuilder(getConfigPath(name)).url(), GetConfigResult.RESPONSE.class);
        configRevisions.putIfAbsent(name, result.revision);
        return result.config;
    }

    public void putConfig(String name, String value) throws XBusException {
        putConfig(name, value, null);
    }

    public void putConfig(String name, String value, Long currentVersion) throws XBusException {
        put(new UrlBuilder(getConfigPath(name)).url(),
                new FormBuilder()
                        .add("value", value)
                        .addIfNotNull("version", currentVersion)
                        .build(),
                PutConfigResult.RESPONSE.class);
    }

    public Config watchConfig(String name) throws XBusException {
        return watchConfig(name, null);
    }

    public Config watchConfig(String name, Integer timeout) throws XBusException {
        WatchConfigResult result;
        try {
            result = get(new WatchUrlBuilder(getConfigPath(name), configRevisions.get(name), timeout).url(),
                    WatchConfigResult.RESPONSE.class);
        } catch (DeadlineExceededException e) {
            return null;
        }
        configRevisions.put(name, result.revision);
        return result.config;
    }

    public Service getService(String name, String version) throws XBusException {
        GetServiceResult result = get(new UrlBuilder(getServicePath(name, version)).url(),
                GetServiceResult.RESPONSE.class);
        serviceRevisions.putIfAbsent(Service.genId(name, version), result.revision);
        if (!result.service.name.equals(name) || !result.service.version.equals(version)) {
            throw new XBusException(ErrorCode.Unknown, new Exception("unmatched service: " + result.service.toString()));
        }
        return result.service;
    }


    public Service watchService(String name, String version) throws XBusException {
        return watchService(name, version, null);
    }

    public Service watchService(String name, String version, Integer timeout) throws XBusException {
        String id = Service.genId(name, version);
        Long revision = serviceRevisions.get(id);
        WatchServiceResult result;
        try {
            result = get(new WatchUrlBuilder(getServicePath(name, version), revision, timeout).url(),
                    WatchServiceResult.RESPONSE.class);
        } catch (DeadlineExceededException e) {
            return null;
        }
        serviceRevisions.put(id, result.revision);
        if (!result.service.name.equals(name) || !result.service.version.equals(version)) {
            throw new XBusException(ErrorCode.Unknown, new Exception("unmatched service: " + result.service.toString()));
        }
        return result.service;
    }

    public long plugService(ServiceDesc desc, ServiceEndpoint endpoint) throws XBusException {
        return plugService(desc, endpoint, null);
    }

    public long plugService(ServiceDesc desc, ServiceEndpoint endpoint, Integer ttl) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder("/api/services").url(),
                new FormBuilder()
                        .add("desc", gson.toJson(desc))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        return result.leaseId;
    }

    public long plugServices(ServiceDesc[] desces, ServiceEndpoint endpoint, Integer ttl) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder("/api/services").url(),
                new FormBuilder()
                        .add("desces", gson.toJson(desces))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        return result.leaseId;
    }

    public void unplugService(String name, String version) throws XBusException {
        String address = addresses.get(Service.genId(name, version));
        if (address == null) {
            throw new RuntimeException("missing address for " + Service.genId(name, version));
        }
        delete(new UrlBuilder(getServicePath(name, version)).url(), VoidResult.RESPONSE.class);
    }

    public void revokeLease(long leaseId) throws XBusException {
        delete(new UrlBuilder("/api/leases/" + leaseId).url(), VoidResult.RESPONSE.class);
    }

    public void keepAliveLease(long leaseId) throws XBusException {
        post(new UrlBuilder("/api/leases/" + leaseId).url(), null, VoidResult.RESPONSE.class);
    }

    public void keepAliveService(String name, String version) throws XBusException {
        String id = Service.genId(name, version);
        String address = addresses.get(id);
        if (address == null) {
            throw new RuntimeException("missing address for " + id);
        }
        Long keepId = leaseIds.get(id);
        if (keepId == null) {
            throw new RuntimeException("missing keep id for " + id);
        }
        put(new UrlBuilder(getServicePath(name, version)).url(),
                new FormBuilder().add("keep_id", keepId).build(),
                VoidResult.RESPONSE.class);
    }

    public void updateServiceConfig(String name, String version, String config) throws XBusException {
        String id = Service.genId(name, version);
        String address = addresses.get(id);
        if (address == null) {
            throw new RuntimeException("missing address for " + id);
        }
        Long keepId = leaseIds.get(id);
        if (keepId == null) {
            throw new RuntimeException("missing keep id for " + id);
        }
        ServiceEndpoint endpoint = new ServiceEndpoint(null, config);
        put(new UrlBuilder(getServicePath(name, version)).url(),
                new FormBuilder().add("endpoint", gson.toJson(endpoint)).build(),
                VoidResult.RESPONSE.class);
    }

    public ServiceSession newServiceSession() {
        return new ServiceSession(this);
    }
}