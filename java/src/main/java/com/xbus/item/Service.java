package com.xbus.item;

/**
 * Created by lolynx on 6/14/16.
 */
public class Service extends ServiceDesc {
    public ServiceEndpoint[] endpoints;

    public Service() {
    }

    public Service(String name, String version, String type, ServiceEndpoint[] endpoints) {
        super(type, null, null);
        this.name = name;
        this.version = version;
        this.endpoints = endpoints;
    }
}