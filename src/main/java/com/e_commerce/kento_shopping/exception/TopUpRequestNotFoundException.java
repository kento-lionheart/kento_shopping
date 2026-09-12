package com.e_commerce.kento_shopping.exception;

public class TopUpRequestNotFoundException extends RuntimeException {
    public TopUpRequestNotFoundException(String message) {
        super(message);
    }
}
