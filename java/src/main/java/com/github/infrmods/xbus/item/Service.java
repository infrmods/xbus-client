package com.github.infrmods.xbus.item;

/**
 * Created by lolynx on 6/14/16.
 */
public class Service extends ServiceDesc {
    public ServiceEndpoint[] endpoints;

    public Service() {
    }

    public Service(String name, String version, String type, String proto, ServiceEndpoint[] endpoints) {
        super(name, version, type, proto);
        this.endpoints = endpoints;
    }
}