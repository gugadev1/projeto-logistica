package com.projetologistica.web;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.projetologistica.exception.CapacidadeExcedidaException;
import com.projetologistica.model.Pacote;
import com.projetologistica.model.Veiculo;
import com.projetologistica.service.RoteirizadorDistribuicao;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ServidorDashboard {
    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/simular", new SimulacaoHandler());
        server.createContext("/", new StaticResourceHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Dashboard running at http://localhost:" + port);
    }

    static class SimulacaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    writeJson(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                SimulacaoRequest request = GSON.fromJson(requestBody, SimulacaoRequest.class);
                validarRequest(request);

                SimulacaoResponse response = processarSimulacao(request);
                writeJson(exchange, 200, response);
            } catch (CapacidadeExcedidaException e) {
                writeJson(exchange, 409, Map.of(
                        "error", "CAPACIDADE_EXCEDIDA",
                        "message", e.getMessage()
                ));
            } catch (IllegalArgumentException | JsonSyntaxException e) {
                writeJson(exchange, 400, Map.of(
                        "error", "REQUISICAO_INVALIDA",
                        "message", e.getMessage()
                ));
            } catch (Exception e) {
                writeJson(exchange, 500, Map.of(
                        "error", "ERRO_INTERNO",
                        "message", "Falha inesperada no processamento da simulacao."
                ));
            }
        }

        private SimulacaoResponse processarSimulacao(SimulacaoRequest request) {
            Veiculo veiculo = new Veiculo(
                    request.placa,
                    request.cargaMax,
                    request.volumeMax,
                    request.cargaInicial
            );

            List<Pacote> pacotes = new ArrayList<>();
            for (PacoteRequest pacoteRequest : request.pacotes) {
                Pacote pacote = new Pacote(
                        pacoteRequest.id,
                        pacoteRequest.peso,
                        pacoteRequest.volume,
                        pacoteRequest.coordenadaX,
                        pacoteRequest.coordenadaY,
                        parsePrioridade(pacoteRequest.prioridade)
                );

                veiculo.adicionarPacote(pacote);
                pacotes.add(pacote);
            }

            RoteirizadorDistribuicao roteirizador = new RoteirizadorDistribuicao();
            List<Pacote> romaneioOrdenado = roteirizador.ordenarSaida(pacotes, request.origemX, request.origemY);

            List<ItemRomaneio> itens = new ArrayList<>();
            for (Pacote pacote : romaneioOrdenado) {
                double distancia = roteirizador.calcularDistanciaEuclidiana(
                        request.origemX,
                        request.origemY,
                        pacote.getCoordenadaX(),
                        pacote.getCoordenadaY()
                );
                itens.add(new ItemRomaneio(
                        pacote.getId(),
                        pacote.isExpresso() ? "EXPRESSO" : "PADRAO",
                        distancia,
                        pacote.getPeso(),
                        pacote.getVolume()
                ));
            }

            double ocupacaoPercentual = (veiculo.getCargaAtual() / veiculo.getCargaMax()) * 100.0;
            return new SimulacaoResponse(
                    veiculo.getPlaca(),
                    veiculo.getCargaAtual(),
                    veiculo.getCargaMax(),
                    veiculo.getVolumeAtual(),
                    veiculo.getVolumeMax(),
                    ocupacaoPercentual,
                    itens
            );
        }

        private int parsePrioridade(String prioridade) {
            if (prioridade == null || prioridade.isBlank()) {
                throw new IllegalArgumentException("Prioridade do pacote e obrigatoria.");
            }
            if ("EXPRESSO".equalsIgnoreCase(prioridade.trim())) {
                return Pacote.PRIORIDADE_EXPRESSO;
            }
            try {
                return Integer.parseInt(prioridade.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Prioridade invalida. Use EXPRESSO ou valores de 1 a 5.");
            }
        }

        private void validarRequest(SimulacaoRequest request) {
            if (request == null) {
                throw new IllegalArgumentException("Corpo da requisicao ausente.");
            }
            if (request.placa == null || request.placa.isBlank()) {
                throw new IllegalArgumentException("Placa e obrigatoria.");
            }
            if (request.pacotes == null || request.pacotes.isEmpty()) {
                throw new IllegalArgumentException("Informe ao menos um pacote para simulacao.");
            }
        }
    }

    static class StaticResourceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                path = "/index.html";
            }

            String resourcePath = "web" + path;
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (in == null) {
                byte[] notFound = "Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound);
                }
                return;
            }

            byte[] content = in.readAllBytes();
            in.close();

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType(path));
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }

        private String contentType(String path) {
            if (path.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (path.endsWith(".js")) {
                return "application/javascript; charset=utf-8";
            }
            if (path.endsWith(".html")) {
                return "text/html; charset=utf-8";
            }
            return "text/plain; charset=utf-8";
        }
    }

    private static void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    static class SimulacaoRequest {
        String placa;
        double cargaMax;
        double volumeMax;
        double cargaInicial;
        double origemX;
        double origemY;
        List<PacoteRequest> pacotes;
    }

    static class PacoteRequest {
        String id;
        double peso;
        double volume;
        double coordenadaX;
        double coordenadaY;
        String prioridade;
    }

    static class SimulacaoResponse {
        String placa;
        double cargaAtual;
        double cargaMax;
        double volumeAtual;
        double volumeMax;
        double ocupacaoPercentual;
        List<ItemRomaneio> romaneio;

        SimulacaoResponse(
                String placa,
                double cargaAtual,
                double cargaMax,
                double volumeAtual,
                double volumeMax,
                double ocupacaoPercentual,
                List<ItemRomaneio> romaneio
        ) {
            this.placa = placa;
            this.cargaAtual = cargaAtual;
            this.cargaMax = cargaMax;
            this.volumeAtual = volumeAtual;
            this.volumeMax = volumeMax;
            this.ocupacaoPercentual = ocupacaoPercentual;
            this.romaneio = romaneio;
        }
    }

    static class ItemRomaneio {
        String id;
        String prioridade;
        double distancia;
        double peso;
        double volume;

        ItemRomaneio(String id, String prioridade, double distancia, double peso, double volume) {
            this.id = id;
            this.prioridade = prioridade;
            this.distancia = distancia;
            this.peso = peso;
            this.volume = volume;
        }
    }
}
