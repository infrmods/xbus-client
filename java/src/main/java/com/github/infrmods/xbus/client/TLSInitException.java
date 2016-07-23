package com.github.infrmods.xbus.client;

/**
 * Created by lolynx on 6/11/16.
 */
public class TLSInitException extends Exception {
    TLSInitException(Exception e) {
        super(e);
    }

    TLSInitException(String message) {
        super(message);
    }
}
