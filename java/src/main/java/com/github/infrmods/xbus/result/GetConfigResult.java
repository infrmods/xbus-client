package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.item.Config;

/**
 * Created by lolynx on 6/11/16.
 */
public class GetConfigResult extends Result {
    public static class RESPONSE extends Response<GetConfigResult>{}

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
