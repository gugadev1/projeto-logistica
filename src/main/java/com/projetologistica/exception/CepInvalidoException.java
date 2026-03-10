package com.projetologistica.exception;

public class CepInvalidoException extends RuntimeException {
    public CepInvalidoException(String cep) {
        super("CEP informado e invalido: '" + cep + "'. Informe apenas os 8 digitos numericos.");
    }
}
