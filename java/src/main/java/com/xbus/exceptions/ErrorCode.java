package com.xbus.exceptions;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by lolynx on 6/11/16.
 */
public enum ErrorCode {
    IOError("JAVA_IO_ERROR"),
    Unknown("UNKNOWN_ERROR"),
    SystemError("SYSTEM_ERROR"),
    InvalidParam("INVALID_PARAM"),
    MissingParam("MISSING_PARAM"),
    InvalidName("INVALID_NAME"),
    InvalidVersion("INVALID_VERSION"),
    InvalidAddress("INVALID_ADDRESS"),
    InvalidEndpoint("INVALID_ENDPOINT"),
    DamagedEndpointValue("DAMAGED_ENDPOINT_VALUE"),
    TooManyAttempts("TOO_MANY_ATTEMPTS"),
    NotFound("NOT_FOUND"),
    DeadlineExceeded("DEADLINE_EXCEEDED"),
    Canceled("CANCELED"),
    Deleted("DELETED"),
    ChangedServiceDesc("CHANGED_SERVICE_DESC"),
    NameDuplicated("NAME_DUPLICATED"),
    NotPermitted("NOT_PERMITTED");

    public final String code;

    private static final Map<String, ErrorCode> codes = new HashMap<String, ErrorCode>();

    static {
        for (ErrorCode code : ErrorCode.values()) {
            codes.put(code.code, code);
        }
    }

    ErrorCode(String code) {
        this.code = code;
    }

    public static ErrorCode getErrorCode(String code) {
        return codes.get(code);
    }
}