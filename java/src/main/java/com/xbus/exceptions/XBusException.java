package com.xbus.exceptions;

import com.google.api.client.util.Key;

/**
 * Created by lolynx on 6/11/16.
 */
public class XBusException extends Exception {
    @Key
    public ErrorCode code;

    @Key
    public String message;

    XBusException(ErrorCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public XBusException(ErrorCode code, Exception e) {
        super(e);
        this.code = code;
        this.message = e.getMessage();
    }

    public static XBusException newException(ErrorCode code, String message) {
        switch (code) {
            case DeadlineExceeded:
                return new DeadlineExceededException(message);
            case NotFound:
                return new NotFoundException(message);
            case Deleted:
                return new DeletedException(message);
            default:
                return new XBusException(code, message);
        }
    }
}
