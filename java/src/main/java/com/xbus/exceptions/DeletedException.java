package com.xbus.exceptions;

/**
 * Created by lolynx on 6/14/16.
 */
public class DeletedException extends XBusException {
    DeletedException(String message) {
        super(ErrorCode.Deleted, message);
    }
}
