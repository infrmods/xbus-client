package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.item.Service;

/**
 * Created by lolynx on 6/14/16.
 */
public class GetServiceResult extends Result {
    public static final class RESPONSE extends Response<GetServiceResult> {}

    public Service service;

    public long revision;
}
