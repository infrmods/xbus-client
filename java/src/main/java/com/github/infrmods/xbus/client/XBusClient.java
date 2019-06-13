package com.github.infrmods.xbus.client;

import com.github.infrmods.xbus.exceptions.DeadlineExceededException;
import com.github.infrmods.xbus.item.*;
import com.github.infrmods.xbus.result.*;
import com.github.infrmods.xbus.exceptions.ErrorCode;
import com.github.infrmods.xbus.exceptions.XBusException;

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

    private String getServicePath(String service) {
        return "/api/v1/services/" + service;
    }

    private String getServiceZonePath(String service, String zone) {
        return "/api/v1/services/" + service + "/" + zone;
    }

    public XBusClient(XBusConfig config) throws TLSInitException {
        super(config);
    }

    public Config getConfig(String name) throws XBusException {
        GetConfigResult result = get(new UrlBuilder(getConfigPath(name)).url(), GetConfigResult.RESPONSE.class);
        configRevisions.putIfAbsent(name, result.getRevision());
        return result.getConfig();
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
        configRevisions.put(name, result.getRevision());
        return result.getConfig();
    }

    public Service getService(String serviceKey) throws XBusException {
        GetServiceResult result = get(new UrlBuilder(getServicePath(serviceKey)).url(),
                GetServiceResult.RESPONSE.class);
        Service service = result.getService();
        if (!service.getService().equals(serviceKey)) {
            throw new XBusException(ErrorCode.Unknown, new Exception("unmatched service: " + result.getService().toString()));
        }
        serviceRevisions.putIfAbsent(service.getService(), result.getRevision());
        return service;
    }


    public Service watchService(String service) throws XBusException {
        return watchService(service, null);
    }

    public Service watchService(String serviceKey, Integer timeout) throws XBusException {
        Long revision = serviceRevisions.get(serviceKey);
        WatchServiceResult result;
        try {
            result = get(new WatchUrlBuilder(getServicePath(serviceKey), revision, timeout).url(),
                    WatchServiceResult.RESPONSE.class, watchClient);
        } catch (DeadlineExceededException e) {
            return null;
        }
        Service service = result.getService();
        if (!service.getService().equals(serviceKey)) {
            throw new XBusException(ErrorCode.Unknown, new Exception("unmatched service: " + service.toString()));
        }
        serviceRevisions.put(service.getService(), result.getRevision());
        return service;
    }

    PlugServiceResult plugWithLease(long leaseId, ServiceDesc desc, ServiceEndpoint endpoint) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder(getServicePath(desc.getService())).url(),
                new FormBuilder()
                        .add("desc", gson.toJson(desc))
                        .add("endpoint", gson.toJson(endpoint))
                        .add("lease_id", leaseId).build(),
                PlugServiceResult.RESPONSE.class);
        addresses.put(desc.getId(), endpoint.getAddress());
        return result;
    }

    PlugServiceResult plugAllWithLease(Long leaseId, Integer ttl, ServiceDesc[] desces, ServiceEndpoint endpoint) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder("/api/v1/services").url(),
                new FormBuilder()
                        .add("desces", gson.toJson(desces))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("lease_id", leaseId)
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        for (ServiceDesc desc : desces) {
            addresses.put(desc.getId(), endpoint.getAddress());
        }
        return result;
    }

    public long plugService(ServiceDesc desc, ServiceEndpoint endpoint) throws XBusException {
        return plugService(desc, endpoint, null);
    }

    public long plugService(ServiceDesc desc, ServiceEndpoint endpoint, Integer ttl) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder(getServicePath(desc.getService())).url(),
                new FormBuilder()
                        .add("desc", gson.toJson(desc))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        leaseIds.put(desc.getId(), result.getLeaseId());
        addresses.put(desc.getId(), endpoint.getAddress());
        return result.getLeaseId();
    }

    public long plugServices(ServiceDesc[] desces, ServiceEndpoint endpoint, Integer ttl) throws XBusException {
        PlugServiceResult result = post(
                new UrlBuilder("/api/v1/services").url(),
                new FormBuilder()
                        .add("desces", gson.toJson(desces))
                        .add("endpoint", gson.toJson(endpoint))
                        .addIfNotNull("ttl", ttl).build(),
                PlugServiceResult.RESPONSE.class);
        for (ServiceDesc desc : desces) {
            leaseIds.put(desc.getId(), result.getLeaseId());
            addresses.put(desc.getId(), endpoint.getAddress());
        }
        return result.getLeaseId();
    }

    public void unplugService(String service, String zone) throws XBusException {
        String serviceId = ZoneService.genId(service, zone);
        leaseIds.remove(serviceId);
        String address = addresses.remove(serviceId);
        delete(new UrlBuilder(getServiceZonePath(service, zone) + "/" + address).url(), VoidResult.RESPONSE.class);
    }

    public LeaseGrantResult grantLease(Integer ttl) throws XBusException {
        return post(
                new UrlBuilder("/api/leases").url(),
                new FormBuilder().add("ttl", ttl).build(),
                LeaseGrantResult.RESPONSE.class);
    }

    public void revokeLease(long leaseId) throws XBusException {
        delete(new UrlBuilder("/api/leases/" + leaseId).url(), VoidResult.RESPONSE.class);
    }

    public void keepAliveLease(long leaseId) throws XBusException {
        post(new UrlBuilder("/api/leases/" + leaseId).url(), null, VoidResult.RESPONSE.class);
    }

    public ServiceSession newServiceSession(ServiceEndpoint endpoint, int ttl) throws XBusException {
        return new ServiceSession(this, endpoint, ttl);
    }

    public ServiceSession newServiceSession(ServiceEndpoint endpoint, int ttl, int keepInterval) throws XBusException {
        return new ServiceSession(this, endpoint, ttl, keepInterval);
    }
}