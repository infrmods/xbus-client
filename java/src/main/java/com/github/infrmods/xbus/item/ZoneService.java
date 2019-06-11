package com.github.infrmods.xbus.item;

/**
 * Created by lolynx on 6/14/16.
 */
public class ZoneService extends ServiceDesc {
    private ServiceEndpoint[] endpoints;

    public ZoneService() {
    }

    public ZoneService(String service, String zone, String type, String proto, ServiceEndpoint[] endpoints) {
        super(service, zone, type, proto);
        this.endpoints = endpoints;
    }

    public ServiceEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ServiceEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }
}