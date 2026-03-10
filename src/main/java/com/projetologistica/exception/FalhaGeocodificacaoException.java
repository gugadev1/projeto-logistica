package com.projetologistica.exception;

public class FalhaGeocodificacaoException extends RuntimeException {
    public FalhaGeocodificacaoException(String message) {
        super(message);
    }

    public FalhaGeocodificacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
