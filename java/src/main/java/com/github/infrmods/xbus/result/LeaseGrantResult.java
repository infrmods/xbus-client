package com.github.infrmods.xbus.result;

import com.google.gson.annotations.SerializedName;

public class LeaseGrantResult extends Result {
    public static final class RESPONSE extends Response<LeaseGrantResult>{}

    @SerializedName("lease_id")
    public long leaseId;

    public long ttl;
}
