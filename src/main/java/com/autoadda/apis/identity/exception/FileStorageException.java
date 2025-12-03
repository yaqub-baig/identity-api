package com.autoadda.apis.identity.exception;

import java.io.IOException;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String couldNotInitializeStorageDirectories, IOException e) {
        super(couldNotInitializeStorageDirectories, e);
    }

    public FileStorageException(String message) {
        super(message);
    }
}
