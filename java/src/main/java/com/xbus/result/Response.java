package com.xbus.result;

import com.google.api.client.util.Key;
import com.xbus.exceptions.ErrorCode;
import com.xbus.exceptions.XBusException;

/**
 * Created by lolynx on 6/11/16.
 */
public class Response<T extends Result> {
    public static class Error {
        @Key
        String code;

        @Key
        String message;
    }

    @Key
    public boolean ok;

    @Key
    public T result;

    @Key
    public Error error;

    public T getResult() throws XBusException {
        if (ok) {
            return result;
        }
        if (error != null) {
            ErrorCode errorCode = ErrorCode.getErrorCode(error.code);
            if (errorCode != null) {
                throw XBusException.newException(errorCode, error.message);
            }
            throw XBusException.newException(ErrorCode.Unknown, "[" + error.code + "]: " + error.message);
        }
        throw XBusException.newException(ErrorCode.Unknown, "");
    }
}