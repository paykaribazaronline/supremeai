package com.supremeai.resilience;

public class BlockingOperationException extends RuntimeException {
    public BlockingOperationException(String message) {
        super(message);
    }
    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
