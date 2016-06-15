package com.xbus.result;

import com.google.api.client.util.Key;

/**
 * Created by lolynx on 6/14/16.
 */
public class PlugServiceResult extends Result {
    public static final class RESPONSE extends Response<PlugServiceResult>{}

    @Key
    public long keepId;
}
