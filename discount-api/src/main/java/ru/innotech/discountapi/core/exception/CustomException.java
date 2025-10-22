package ru.innotech.discountapi.core.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{
    private final String[] args;

    public CustomException(String message, String... args) {
        super(message);
        this.args = args;
    }
}
