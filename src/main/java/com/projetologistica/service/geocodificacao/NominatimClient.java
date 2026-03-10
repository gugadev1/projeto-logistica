package com.projetologistica.service.geocodificacao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.projetologistica.exception.FalhaGeocodificacaoException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class NominatimClient implements GeocodificacaoClient {

    private static final String BASE_URL =
            "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "projeto-logistica/0.1.0 (portfolio)";

    private final HttpClient httpClient;
    private final Gson gson;

    public NominatimClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    NominatimClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.gson = new Gson();
    }

    @Override
    public double[] geocodificar(String enderecoQuery) {
        String encodedQuery = URLEncoder.encode(enderecoQuery, StandardCharsets.UTF_8);
        String url = String.format(BASE_URL, encodedQuery);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new FalhaGeocodificacaoException(
                        "Geocodificador retornou status " + response.statusCode() + ".");
            }

            JsonArray results = gson.fromJson(response.body(), JsonArray.class);
            if (results == null || results.isEmpty()) {
                throw new FalhaGeocodificacaoException(
                        "Nenhum resultado de geocodificacao para o endereco informado.");
            }

            JsonObject first = results.get(0).getAsJsonObject();
            double lat = Double.parseDouble(first.get("lat").getAsString());
            double lon = Double.parseDouble(first.get("lon").getAsString());

            // lon -> X (leste-oeste), lat -> Y (norte-sul)
            return new double[]{lon, lat};
        } catch (FalhaGeocodificacaoException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new FalhaGeocodificacaoException("Servico de geocodificacao indisponivel no momento.", e);
        }
    }
}
