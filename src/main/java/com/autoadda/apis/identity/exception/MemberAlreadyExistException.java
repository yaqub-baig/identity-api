package com.autoadda.apis.identity.exception;

/**
 * throw exception when username already exist
 */
public class MemberAlreadyExistException extends Exception {


    public MemberAlreadyExistException() {
        super();
    }

    public MemberAlreadyExistException(String message) {
        super(message);

    }

    public MemberAlreadyExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
