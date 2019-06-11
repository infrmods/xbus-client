package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.item.Config;

/**
 * Created by lolynx on 6/14/16.
 */
public class WatchConfigResult extends Result {
    public static class RESPONSE extends Response<WatchConfigResult> {}

    private Config config;
    private long revision;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}
