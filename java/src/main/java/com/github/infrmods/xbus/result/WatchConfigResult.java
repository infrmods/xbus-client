package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.item.Config;

/**
 * Created by lolynx on 6/14/16.
 */
public class WatchConfigResult extends Result {
    public static class RESPONSE extends Response<WatchConfigResult> {}

    public Config config;

    public long revision;
}
