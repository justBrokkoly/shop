package ru.innotech.discountapi.core.exception;

public class TooManyRequestsException extends CustomException {
    private static final String DEFAULT_MESSAGE = "Too many requests! Try again later";

    public TooManyRequestsException() {
        super(DEFAULT_MESSAGE);
    }

    public TooManyRequestsException(String message, String... args) {
        super(message, args);
    }
}
