package com.xbus.result;

import com.google.api.client.util.Key;

/**
 * Created by lolynx on 6/11/16.
 */
public class Response<T extends Result> {
    @Key
    public boolean ok;

    @Key
    public T result;

    @Key
    public XBusException XBusException;

    public T getResult() throws XBusException {
        if (ok) {
            return result;
        }
        if (XBusException != null) {
            throw XBusException;
        }
        throw new XBusException(ErrorCode.Unknown, "");
    }
}