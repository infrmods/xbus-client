package com.xbus.item;

import com.google.api.client.util.Key;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/11/16.
 */
public class Config {
    @Key
    public String name;

    @Key
    public String value;

    @Key
    public long version;

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
