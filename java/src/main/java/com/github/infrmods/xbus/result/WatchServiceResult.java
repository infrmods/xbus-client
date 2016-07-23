package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.item.Service;

/**
 * Created by lolynx on 6/14/16.
 */
public class WatchServiceResult extends Result {
    public static final class RESPONSE extends Response<WatchServiceResult> {}

    public Service service;

    public long revision;
}
