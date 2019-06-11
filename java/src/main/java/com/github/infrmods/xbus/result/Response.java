package com.github.infrmods.xbus.result;

import com.github.infrmods.xbus.exceptions.ErrorCode;
import com.github.infrmods.xbus.exceptions.XBusException;

/**
 * Created by lolynx on 6/11/16.
 */
public class Response<T extends Result> {
    public static class Error {
        String code;

        String message;
    }

    private boolean ok;

    private T result;

    private Error error;

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

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}