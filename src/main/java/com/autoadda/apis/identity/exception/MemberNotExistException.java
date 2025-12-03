package com.autoadda.apis.identity.exception;

/**
 * throw exception when username not exist
 */
public class MemberNotExistException extends Exception {


    public MemberNotExistException() {
        super();
    }

    public MemberNotExistException(String message) {
        super(message);

    }

    public MemberNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
