package com.projetologistica.service.geocodificacao;

@FunctionalInterface
public interface CepLookupClient {
    RespostaViaCep consultar(String cep);
}
