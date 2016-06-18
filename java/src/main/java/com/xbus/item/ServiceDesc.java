package com.xbus.item;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/14/16.
 */
public class ServiceDesc {
    public String name;

    public String version;

    public String type;

    public String proto;

    public String description;

    public ServiceDesc() {
    }

    public ServiceDesc(String type, String proto, String description) {
        this.type = type;
        this.proto = proto;
        this.description = description;
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
