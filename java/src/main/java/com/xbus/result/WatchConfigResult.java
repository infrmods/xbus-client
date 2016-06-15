package com.xbus.result;

import com.google.api.client.util.Key;
import com.xbus.item.Config;

/**
 * Created by lolynx on 6/14/16.
 */
public class WatchConfigResult extends Result {
    public static class RESPONSE extends Response<WatchConfigResult> {}

    @Key
    public Config config;

    @Key
    public long revision;
}
