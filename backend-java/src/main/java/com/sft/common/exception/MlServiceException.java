package com.sft.common.exception;

/**
 * El ML service no respondió correctamente (timeout, caída, 4xx/5xx, circuito abierto...).
 * Es una excepción INTERNA: TransactionService la captura y usa el fallback.
 * Nunca debe llegar al GlobalExceptionHandler ni convertirse en un 500 para el cliente.
 */
public class MlServiceException extends RuntimeException {
    public MlServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MlServiceException(String message) {
        super(message);
    }
}
