package com.github.infrmods.xbus.client;

import com.github.infrmods.xbus.exceptions.DeadlineExceededException;
import com.github.infrmods.xbus.item.Config;
import com.github.infrmods.xbus.item.Service;
import com.github.infrmods.xbus.item.ServiceDesc;
import com.github.infrmods.xbus.result.*;
import com.github.infrmods.xbus.exceptions.ErrorCode;
import com.github.infrmods.xbus.exceptions.XBusException;
import com.github.infrmods.xbus.item.ServiceEndpoint;

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

    public XBusClient(XBusConfig config) throws TLSInitException {
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

    PlugServiceResult plugWithLease(long leaseId, ServiceDesc desc, ServiceEndpoint endpoint) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder(getServicePath(desc.name, desc.version)).url(),
                new FormBuilder()
                        .add("desc", gson.toJson(desc))
                        .add("endpoint", gson.toJson(endpoint))
                        .add("lease_id", leaseId).build(),
                PlugServiceResult.RESPONSE.class);
        addresses.put(desc.getId(), endpoint.address);
        return result;
    }

    PlugServiceResult plugAllWithLease(Long leaseId, Integer ttl, ServiceDesc[] desces, ServiceEndpoint endpoint) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder("/api/services").url(),
                new FormBuilder()
                        .add("desces", gson.toJson(desces))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("lease_id", leaseId)
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        for (ServiceDesc desc : desces) {
            addresses.put(desc.getId(), endpoint.address);
        }
        return result;
    }

    public long plugService(ServiceDesc desc, ServiceEndpoint endpoint) throws XBusException {
        return plugService(desc, endpoint, null);
    }

    public long plugService(ServiceDesc desc, ServiceEndpoint endpoint, Integer ttl) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder(getServicePath(desc.name, desc.version)).url(),
                new FormBuilder()
                        .add("desc", gson.toJson(desc))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        leaseIds.put(desc.getId(), result.leaseId);
        addresses.put(desc.getId(), endpoint.address);
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
        for (ServiceDesc desc : desces) {
            leaseIds.put(desc.getId(), result.leaseId);
            addresses.put(desc.getId(), endpoint.address);
        }
        return result.leaseId;
    }

    public void unplugService(String name, String version) throws XBusException {
        String serviceId = Service.genId(name, version);
        leaseIds.remove(serviceId);
        addresses.remove(serviceId);
        delete(new UrlBuilder(getServicePath(name, version)).url(), VoidResult.RESPONSE.class);
    }

    public LeaseGrantResult grantLease(Integer ttl) throws XBusException {
        LeaseGrantResult result = post(new UrlBuilder("/api/leases").url(), null, LeaseGrantResult.RESPONSE.class);
        return result;
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
        Long leaseId = leaseIds.get(id);
        if (leaseId == null) {
            throw new RuntimeException("missing keep id for " + id);
        }
        keepAliveLease(leaseId);
    }

    public void updateServiceConfig(String name, String version, String config) throws XBusException {
        String id = Service.genId(name, version);
        String address = addresses.get(id);
        if (address == null) {
            throw new RuntimeException("missing address for " + id);
        }
        Long leaseId = leaseIds.get(id);
        if (leaseId == null) {
            throw new RuntimeException("missing keep id for " + id);
        }
        ServiceEndpoint endpoint = new ServiceEndpoint(null, config);
        put(new UrlBuilder(getServicePath(name, version)).url(),
                new FormBuilder().add("endpoint", gson.toJson(endpoint)).build(),
                VoidResult.RESPONSE.class);
    }

    public ServiceSession newServiceSession(ServiceEndpoint endpoint, int ttl) throws XBusException {
        return new ServiceSession(this, endpoint, ttl);
    }
}