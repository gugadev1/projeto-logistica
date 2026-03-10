package com.projetologistica.service.geocodificacao;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.projetologistica.exception.CepNaoEncontradoException;
import com.projetologistica.exception.FalhaGeocodificacaoException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ViaCepClient implements CepLookupClient {

    private static final String BASE_URL = "https://viacep.com.br/ws/%s/json/";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final Gson gson;

    public ViaCepClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    ViaCepClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.gson = new Gson();
    }

    @Override
    public RespostaViaCep consultar(String cep) {
        String url = String.format(BASE_URL, cep);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 400) {
                throw new CepNaoEncontradoException(cep);
            }
            if (response.statusCode() != 200) {
                throw new FalhaGeocodificacaoException("ViaCEP retornou status inesperado: " + response.statusCode());
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.has("erro") && !json.get("erro").isJsonNull() && json.get("erro").getAsBoolean()) {
                throw new CepNaoEncontradoException(cep);
            }

            return new RespostaViaCep(
                    getStringOrEmpty(json, "cep"),
                    getStringOrEmpty(json, "logradouro"),
                    getStringOrEmpty(json, "bairro"),
                    getStringOrEmpty(json, "localidade"),
                    getStringOrEmpty(json, "uf")
            );
        } catch (CepNaoEncontradoException | FalhaGeocodificacaoException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new FalhaGeocodificacaoException("Servico ViaCEP indisponivel no momento.", e);
        }
    }

    private String getStringOrEmpty(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
}
