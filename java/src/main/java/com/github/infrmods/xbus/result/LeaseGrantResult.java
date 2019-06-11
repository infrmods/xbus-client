package com.github.infrmods.xbus.result;

import com.google.gson.annotations.SerializedName;

public class LeaseGrantResult extends Result {
    public static final class RESPONSE extends Response<LeaseGrantResult>{}

    @SerializedName("lease_id")
    private long leaseId;
    private long ttl;

    public long getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(long leaseId) {
        this.leaseId = leaseId;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
