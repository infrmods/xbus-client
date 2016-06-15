package com.xbus.exceptions;

/**
 * Created by lolynx on 6/14/16.
 */
public class NotFoundException extends XBusException {
    NotFoundException(String message) {
        super(ErrorCode.NotFound, message);
    }
}