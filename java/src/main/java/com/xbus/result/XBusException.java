package com.xbus.result;

import com.google.api.client.util.Key;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusException extends Exception {
    @Key
    public ErrorCode code;

    @Key
    public String message;

    public XBusException(ErrorCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public XBusException(ErrorCode code, Exception e) {
        super(e);
        this.code = code;
        this.message = e.getMessage();
    }
}
