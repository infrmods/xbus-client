package com.github.infrmods.xbus.item;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/14/16.
 */
public class ServiceDesc {
    public static final String DEFAULT_ZONE = "default";

    private String service;
    private String zone;
    private String type;
    private String extension;
    private String proto;
    private String description;

    public ServiceDesc() {
    }

    public ServiceDesc(String service, String zone, String type, String proto) {
        if (zone == null || zone.length() == 0) {
            zone = DEFAULT_ZONE;
        }

        this.service = service;
        this.zone = zone;
        this.type = type;
        this.proto = proto;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getProto() {
        return proto;
    }

    public void setProto(String proto) {
        this.proto = proto;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public String getId() {
        return genId(service, zone);
    }

    public static String genId(String service, String zone) {
        return service + "#" + zone;
    }
}
