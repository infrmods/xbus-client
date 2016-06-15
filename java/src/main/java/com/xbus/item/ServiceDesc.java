package com.xbus.item;

import com.google.api.client.util.Key;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/14/16.
 */
public class ServiceDesc {
    @Key
    public String type;

    @Key
    public String proto;

    @Key
    public String description;

    public ServiceDesc() {
    }

    public ServiceDesc(String type, String proto, String description) {
        this.type = type;
        this.proto = proto;
        this.description = description;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
