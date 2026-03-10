package com.projetologistica.service.geocodificacao;

public record RespostaViaCep(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf) {}
