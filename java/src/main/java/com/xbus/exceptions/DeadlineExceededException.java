package com.xbus.exceptions;

/**
 * Created by lolynx on 6/14/16.
 */
public class DeadlineExceededException extends XBusException {
    DeadlineExceededException(String message) {
        super(ErrorCode.DeadlineExceeded, message);
    }
}
