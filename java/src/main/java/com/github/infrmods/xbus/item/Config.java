package com.github.infrmods.xbus.item;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/11/16.
 */
public class Config {
    public String name;

    public String value;

    public long version;

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
