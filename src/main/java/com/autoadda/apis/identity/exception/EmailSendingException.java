package com.autoadda.apis.identity.exception;

public class EmailSendingException extends RuntimeException {
    public EmailSendingException(String failedToSendEmail, Exception e) {
        super(failedToSendEmail,e);
    }
}
