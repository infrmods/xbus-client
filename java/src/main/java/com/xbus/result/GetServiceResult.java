package com.xbus.result;

import com.google.api.client.util.Key;
import com.xbus.item.Service;

/**
 * Created by lolynx on 6/14/16.
 */
public class GetServiceResult extends Result {
    public static final class RESPONSE extends Response<GetServiceResult> {}

    @Key
    public Service service;

    @Key
    public long revision;
}
