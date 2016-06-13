package com.xbus.result;

import com.google.api.client.util.Key;
import com.xbus.item.Config;

/**
 * Created by lolynx on 6/11/16.
 */
public class GetConfigResult extends Result {
    public static class RESPONSE extends Response<GetConfigResult>{}

    @Key
    public Config config;

    @Key
    public long revision;
}
