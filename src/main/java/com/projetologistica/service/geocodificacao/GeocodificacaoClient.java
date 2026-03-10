package com.projetologistica.service.geocodificacao;

@FunctionalInterface
public interface GeocodificacaoClient {
    /** Retorna [lon, lat] como double[0]=X e double[1]=Y. */
    double[] geocodificar(String enderecoQuery);
}
