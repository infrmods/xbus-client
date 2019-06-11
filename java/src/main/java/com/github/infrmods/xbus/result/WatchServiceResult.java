package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.item.Service;

/**
 * Created by lolynx on 6/14/16.
 */
public class WatchServiceResult extends Result {
    public static final class RESPONSE extends Response<WatchServiceResult> {}

    private Service service;
    private long revision;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}
