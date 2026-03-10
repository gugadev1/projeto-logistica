package com.projetologistica.exception;

public class CepNaoEncontradoException extends RuntimeException {
    public CepNaoEncontradoException(String cep) {
        super("CEP informado nao foi encontrado: '" + cep + "'. Confira e tente novamente.");
    }
}
