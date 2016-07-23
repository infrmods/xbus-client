package com.github.infrmods.xbus.result;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Created by lolynx on 6/11/16.
 */
public abstract class Result {
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}