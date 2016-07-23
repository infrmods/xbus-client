package com.github.infrmods.xbus.item;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/14/16.
 */
public class ServiceEndpoint {
    public String address;

    public String config;

    public ServiceEndpoint() {
    }

    public ServiceEndpoint(String address, String config) {
        this.address = address;
        this.config = config;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
