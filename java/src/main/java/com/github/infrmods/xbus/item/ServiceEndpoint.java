package com.github.infrmods.xbus.item;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/14/16.
 */
public class ServiceEndpoint {
    private String address;
    private String config;

    public ServiceEndpoint() {
    }

    public ServiceEndpoint(String address, String config) {
        this.address = address;
        this.config = config;
    }

    public String getHost() {
        String []parts = address.split(":");
        if (parts.length > 1) {
            return parts[parts.length - 2];
        }
        return address;
    }

    public Integer getPort() {
        String []parts = address.split(":");
        if (parts.length > 1) {
            return Integer.valueOf(parts[parts.length - 1]);
        }
        return null;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
