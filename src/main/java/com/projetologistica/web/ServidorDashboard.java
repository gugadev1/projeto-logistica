package com.projetologistica.web;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.projetologistica.exception.CapacidadeExcedidaException;
import com.projetologistica.exception.CepInvalidoException;
import com.projetologistica.exception.CepNaoEncontradoException;
import com.projetologistica.exception.FalhaGeocodificacaoException;
import com.projetologistica.model.Pacote;
import com.projetologistica.model.Veiculo;
import com.projetologistica.persistence.DatabaseManager;
import com.projetologistica.service.HistoricoOperacionalService;
import com.projetologistica.service.RoteirizadorDistribuicao;
import com.projetologistica.service.geocodificacao.CepResolverService;
import com.projetologistica.service.geocodificacao.NominatimClient;
import com.projetologistica.service.geocodificacao.ResolucaoCep;
import com.projetologistica.service.geocodificacao.ViaCepClient;
import com.projetologistica.service.dto.PacotePersistenciaDados;
import com.projetologistica.service.dto.RomaneioPersistenciaDados;
import com.projetologistica.service.dto.SimulacaoPersistenciaDados;
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
        DatabaseManager.initialize();
        HistoricoOperacionalService historicoService = new HistoricoOperacionalService(DatabaseManager.getDataSource());
        CepResolverService cepResolverService = new CepResolverService(
                new ViaCepClient(), new NominatimClient(), DatabaseManager.getDataSource());

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/simular", new SimulacaoHandler(historicoService, cepResolverService));
        server.createContext("/", new StaticResourceHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Dashboard running at http://localhost:" + port);
    }

    static class SimulacaoHandler implements HttpHandler {
        private static final String CANAL_WEB = "WEB";
        private final HistoricoOperacionalService historicoService;
        private final CepResolverService cepResolverService;

        SimulacaoHandler(HistoricoOperacionalService historicoService, CepResolverService cepResolverService) {
            this.historicoService = historicoService;
            this.cepResolverService = cepResolverService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestBody = "";
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    writeJson(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                SimulacaoRequest request = GSON.fromJson(requestBody, SimulacaoRequest.class);
                validarRequest(request);

                ResultadoProcessamento resultado = processarSimulacao(request);
                historicoService.registrarSucesso(CANAL_WEB, requestBody, resultado.persistenciaDados);
                writeJson(exchange, 200, resultado.response);
            } catch (CapacidadeExcedidaException e) {
                historicoService.registrarFalha(CANAL_WEB, requestBody, "FALHA_CAPACIDADE", e.getMessage());
                writeJson(exchange, 409, Map.of(
                        "error", "CAPACIDADE_EXCEDIDA",
                        "message", e.getMessage()
                ));
            } catch (CepInvalidoException | CepNaoEncontradoException e) {
                historicoService.registrarFalha(CANAL_WEB, requestBody, "FALHA_ENDERECO", e.getMessage());
                writeJson(exchange, 422, Map.of(
                        "error", "ENDERECO_INVALIDO",
                        "message", e.getMessage()
                ));
            } catch (FalhaGeocodificacaoException e) {
                historicoService.registrarFalha(CANAL_WEB, requestBody, "FALHA_GEOCODIFICACAO", e.getMessage());
                writeJson(exchange, 503, Map.of(
                        "error", "SERVICO_GEOCODIFICACAO_INDISPONIVEL",
                        "message", e.getMessage()
                ));
            } catch (IllegalArgumentException | JsonSyntaxException e) {
                historicoService.registrarFalha(CANAL_WEB, requestBody, "FALHA_VALIDACAO", e.getMessage());
                writeJson(exchange, 400, Map.of(
                        "error", "REQUISICAO_INVALIDA",
                        "message", e.getMessage()
                ));
            } catch (Exception e) {
                historicoService.registrarFalha(CANAL_WEB, requestBody, "ERRO_INTERNO", e.getMessage());
                writeJson(exchange, 500, Map.of(
                        "error", "ERRO_INTERNO",
                        "message", "Falha inesperada no processamento da simulacao."
                ));
            }
        }

        private ResultadoProcessamento processarSimulacao(SimulacaoRequest request) {
            ResolucaoCep origemResolvida = resolverOrigem(request);
            double origemX = origemResolvida.coordenadaX();
            double origemY = origemResolvida.coordenadaY();

            Veiculo veiculo = new Veiculo(
                    request.placa,
                    request.cargaMax,
                    request.volumeMax,
                    request.cargaInicial
            );

            List<Pacote> pacotes = new ArrayList<>();
            List<PacotePersistenciaDados> pacotesPersistencia = new ArrayList<>();
            for (PacoteRequest pacoteRequest : request.pacotes) {
                int prioridade = parsePrioridade(pacoteRequest.prioridade);
                ResolucaoCep pacoteResolvido = resolverPacote(pacoteRequest);
                Pacote pacote = new Pacote(
                        pacoteRequest.id,
                        pacoteRequest.peso,
                        pacoteRequest.volume,
                        pacoteResolvido.coordenadaX(),
                        pacoteResolvido.coordenadaY(),
                        prioridade
                );

                veiculo.adicionarPacote(pacote);
                pacotes.add(pacote);
                pacotesPersistencia.add(new PacotePersistenciaDados(
                        pacote.getId(),
                        pacote.getPeso(),
                        pacote.getVolume(),
                        pacote.getCoordenadaX(),
                        pacote.getCoordenadaY(),
                        prioridade,
                        "CARREGADO",
                        null,
                        pacoteRequest.cep,
                        pacoteResolvido.fonte()
                ));
            }

            RoteirizadorDistribuicao roteirizador = new RoteirizadorDistribuicao();
            List<Pacote> romaneioOrdenado = roteirizador.ordenarSaida(pacotes, origemX, origemY);

            List<ItemRomaneio> itens = new ArrayList<>();
            List<RomaneioPersistenciaDados> itensPersistencia = new ArrayList<>();
            int ordemSaida = 1;
            for (Pacote pacote : romaneioOrdenado) {
                double distancia = roteirizador.calcularDistanciaEuclidiana(
                        origemX,
                        origemY,
                        pacote.getCoordenadaX(),
                        pacote.getCoordenadaY()
                );
                itens.add(new ItemRomaneio(
                        pacote.getId(),
                        pacote.isExpresso() ? "EXPRESSO" : "PADRAO",
                    ordemSaida,
                        distancia,
                        pacote.getPeso(),
                        pacote.getVolume()
                ));

                itensPersistencia.add(new RomaneioPersistenciaDados(
                    pacote.getId(),
                    ordemSaida,
                    pacote.isExpresso() ? "EXPRESSO" : "PADRAO",
                    distancia
                ));
                ordemSaida++;
            }

            double ocupacaoPercentual = (veiculo.getCargaAtual() / veiculo.getCargaMax()) * 100.0;
                SimulacaoResponse response = new SimulacaoResponse(
                    veiculo.getPlaca(),
                    veiculo.getCargaAtual(),
                    veiculo.getCargaMax(),
                    veiculo.getVolumeAtual(),
                    veiculo.getVolumeMax(),
                    ocupacaoPercentual,
                    itens
            );

                SimulacaoPersistenciaDados persistencia = new SimulacaoPersistenciaDados(
                    veiculo.getPlaca(),
                    veiculo.getCargaMax(),
                    veiculo.getVolumeMax(),
                    request.cargaInicial,
                    origemX,
                    origemY,
                    veiculo.getCargaAtual(),
                    veiculo.getVolumeAtual(),
                    ocupacaoPercentual,
                    pacotesPersistencia,
                    itensPersistencia,
                    request.origemCep,
                    origemResolvida.fonte()
                );

                return new ResultadoProcessamento(response, persistencia);
        }

        private ResolucaoCep resolverOrigem(SimulacaoRequest request) {
            if (request.origemCep != null && !request.origemCep.isBlank()) {
                return cepResolverService.resolver(request.origemCep, request.origemX, request.origemY);
            }
            double x = request.origemX != null ? request.origemX : 0.0;
            double y = request.origemY != null ? request.origemY : 0.0;
            return new ResolucaoCep(x, y, null);
        }

        private ResolucaoCep resolverPacote(PacoteRequest pacoteRequest) {
            if (pacoteRequest.cep != null && !pacoteRequest.cep.isBlank()) {
                return cepResolverService.resolver(
                        pacoteRequest.cep, pacoteRequest.coordenadaX, pacoteRequest.coordenadaY);
            }
            double x = pacoteRequest.coordenadaX != null ? pacoteRequest.coordenadaX : 0.0;
            double y = pacoteRequest.coordenadaY != null ? pacoteRequest.coordenadaY : 0.0;
            return new ResolucaoCep(x, y, null);
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
            boolean origemCepInformado = request.origemCep != null && !request.origemCep.isBlank();
            boolean origemCoordInformada = request.origemX != null && request.origemY != null;
            if (!origemCepInformado && !origemCoordInformada) {
                throw new IllegalArgumentException(
                        "Informe a origem por CEP (origemCep) ou por coordenadas (origemX e origemY).");
            }
            for (int i = 0; i < request.pacotes.size(); i++) {
                PacoteRequest p = request.pacotes.get(i);
                boolean cepPacote = p.cep != null && !p.cep.isBlank();
                boolean coordPacote = p.coordenadaX != null && p.coordenadaY != null;
                if (!cepPacote && !coordPacote) {
                    throw new IllegalArgumentException(
                            "Pacote na posicao " + (i + 1)
                                    + " deve ter CEP (cep) ou coordenadas (coordenadaX e coordenadaY).");
                }
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
        String origemCep;   // opcional – quando presente, CEP é resolvido em coordenadas
        Double origemX;     // obrigatório se origemCep ausente; fallback quando origemCep presente
        Double origemY;
        List<PacoteRequest> pacotes;
    }

    static class PacoteRequest {
        String id;
        double peso;
        double volume;
        String cep;          // opcional – quando presente, CEP é resolvido em coordenadas
        Double coordenadaX;  // obrigatório se cep ausente; fallback quando cep presente
        Double coordenadaY;
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
        int ordem;
        double distancia;
        double peso;
        double volume;

        ItemRomaneio(String id, String prioridade, int ordem, double distancia, double peso, double volume) {
            this.id = id;
            this.prioridade = prioridade;
            this.ordem = ordem;
            this.distancia = distancia;
            this.peso = peso;
            this.volume = volume;
        }
    }

    static class ResultadoProcessamento {
        SimulacaoResponse response;
        SimulacaoPersistenciaDados persistenciaDados;

        ResultadoProcessamento(SimulacaoResponse response, SimulacaoPersistenciaDados persistenciaDados) {
            this.response = response;
            this.persistenciaDados = persistenciaDados;
        }
    }
}
