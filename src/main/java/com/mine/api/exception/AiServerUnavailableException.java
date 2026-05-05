package com.mine.api.exception;

public class AiServerUnavailableException extends RuntimeException {

    public AiServerUnavailableException(String message) {
        super(message);
    }

    public AiServerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
