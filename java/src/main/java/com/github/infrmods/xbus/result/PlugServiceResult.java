package com.github.infrmods.xbus.result;

import com.google.gson.annotations.SerializedName;

/**
 * Created by lolynx on 6/14/16.
 */
public class PlugServiceResult extends Result {
    public static final class RESPONSE extends Response<PlugServiceResult>{}

    @SerializedName("lease_id")
    public long leaseId;

    public long ttl;
}
