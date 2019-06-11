package com.github.infrmods.xbus.result;

/**
 * Created by lolynx on 6/14/16.
 */
public class PutConfigResult extends Result {
    public static final class RESPONSE extends Response<PutConfigResult> {}

    private long revision;

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}