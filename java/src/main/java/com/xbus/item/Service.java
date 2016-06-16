package com.xbus.item;

import com.google.api.client.util.Key;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/14/16.
 */
public class Service extends ServiceDesc {
    @Key
    public String name;

    @Key
    public String version;

    @Key
    public ServiceEndpoint[] endpoints;

    public Service() {
    }

    public Service(String name, String version, String type, ServiceEndpoint[] endpoints) {
        super(type, null, null);
        this.name = name;
        this.version = version;
        this.endpoints = endpoints;
    }

    public ServiceDesc getDesc() {
        return new ServiceDesc(type, proto, description);
    }

    public String getId() {
        return name + ":" + version;
    }

    public static String genId(String name, String version) {
        return name + ":" + version;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}