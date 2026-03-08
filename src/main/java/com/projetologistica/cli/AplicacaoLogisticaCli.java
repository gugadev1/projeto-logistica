package com.projetologistica.cli;

import com.projetologistica.exception.CapacidadeExcedidaException;
import com.projetologistica.model.Pacote;
import com.projetologistica.model.Veiculo;
import com.projetologistica.service.RoteirizadorDistribuicao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class AplicacaoLogisticaCli {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        try (Scanner scanner = criarScannerInterativo()) {
            System.out.println("=== Simulador de Logistica - CLI ===");

            String placa = lerTexto(scanner, "Placa do veiculo: ");
            double cargaMax = lerDouble(scanner, "Capacidade maxima de carga (kg): ");
            double volumeMax = lerDouble(scanner, "Capacidade maxima de volume (m3): ");
            double cargaInicial = lerDouble(scanner, "Carga atual inicial (kg): ");
            double origemX = lerDouble(scanner, "Coordenada X da origem: ");
            double origemY = lerDouble(scanner, "Coordenada Y da origem: ");

            Veiculo veiculo = new Veiculo(placa, cargaMax, volumeMax, cargaInicial);
            RoteirizadorDistribuicao roteirizador = new RoteirizadorDistribuicao();

            int quantidadePacotes = lerInteiro(scanner, "Quantidade de pacotes para processar: ");

            List<Pacote> pacotesCarregados = new ArrayList<>();
            List<String> pacotesRejeitados = new ArrayList<>();

            for (int i = 1; i <= quantidadePacotes; i++) {
                System.out.println();
                System.out.println("Pacote #" + i);

                String id = lerTexto(scanner, "ID: ");
                double peso = lerDouble(scanner, "Peso (kg): ");
                double volume = lerDouble(scanner, "Volume (m3): ");
                double x = lerDouble(scanner, "Coordenada X: ");
                double y = lerDouble(scanner, "Coordenada Y: ");
                int prioridade = lerPrioridade(scanner);

                Pacote pacote = new Pacote(id, peso, volume, x, y, prioridade);

                try {
                    veiculo.adicionarPacote(pacote);
                    pacotesCarregados.add(pacote);
                    System.out.println("Status: carregado com sucesso.");
                } catch (CapacidadeExcedidaException e) {
                    pacotesRejeitados.add(id + " -> " + e.getMessage());
                    System.out.println("Status: rejeitado. Motivo: " + e.getMessage());
                }
            }

            System.out.println();
            System.out.println("=== Resumo Operacional ===");
            System.out.println("Veiculo: " + veiculo.getPlaca());
            System.out.printf("Carga atual: %.2f / %.2f kg%n", veiculo.getCargaAtual(), veiculo.getCargaMax());
            System.out.printf("Volume atual: %.2f / %.2f m3%n", veiculo.getVolumeAtual(), veiculo.getVolumeMax());
            System.out.println("Pacotes carregados: " + pacotesCarregados.size());
            System.out.println("Pacotes rejeitados: " + pacotesRejeitados.size());

            if (!pacotesRejeitados.isEmpty()) {
                System.out.println();
                System.out.println("Rejeicoes:");
                for (String rejeicao : pacotesRejeitados) {
                    System.out.println("- " + rejeicao);
                }
            }

            if (!pacotesCarregados.isEmpty()) {
                List<Pacote> ordemSaida = roteirizador.ordenarSaida(pacotesCarregados, origemX, origemY);

                System.out.println();
                System.out.println("=== Ordem de Saida ===");
                int posicao = 1;
                for (Pacote pacote : ordemSaida) {
                    double distancia = roteirizador.calcularDistanciaEuclidiana(
                            origemX,
                            origemY,
                            pacote.getCoordenadaX(),
                            pacote.getCoordenadaY()
                    );
                    String tipoPrioridade = pacote.isExpresso() ? "EXPRESSO" : "PADRAO";
                    System.out.printf(
                            "%d. %s | %s | Distancia: %.2f%n",
                            posicao,
                            pacote.getId(),
                            tipoPrioridade,
                            distancia
                    );
                    posicao++;
                }
            } else {
                System.out.println();
                System.out.println("Nenhum pacote foi carregado para roteirizacao.");
            }
        }
    }

    private static String lerTexto(Scanner scanner, String prompt) {
        while (true) {
            exibirPrompt(prompt);
            String valor = scanner.nextLine().trim();
            if (!valor.isEmpty()) {
                return valor;
            }
            System.out.println("Entrada invalida. Informe um texto nao vazio.");
        }
    }

    private static int lerInteiro(Scanner scanner, String prompt) {
        while (true) {
            exibirPrompt(prompt);
            String entrada = scanner.nextLine().trim();
            try {
                int valor = Integer.parseInt(entrada);
                if (valor >= 0) {
                    return valor;
                }
            } catch (NumberFormatException ignored) {
                // Keep loop until a valid value is provided.
            }
            System.out.println("Entrada invalida. Informe um numero inteiro >= 0.");
        }
    }

    private static double lerDouble(Scanner scanner, String prompt) {
        while (true) {
            exibirPrompt(prompt);
            String entrada = scanner.nextLine().trim().replace(',', '.');
            try {
                double valor = Double.parseDouble(entrada);
                if (valor >= 0) {
                    return valor;
                }
            } catch (NumberFormatException ignored) {
                // Keep loop until a valid value is provided.
            }
            System.out.println("Entrada invalida. Informe um numero >= 0.");
        }
    }

    private static int lerPrioridade(Scanner scanner) {
        while (true) {
            exibirPrompt("Prioridade (EXPRESSO ou 1-5): ");
            String entrada = scanner.nextLine().trim().toUpperCase(Locale.ROOT);

            if ("EXPRESSO".equals(entrada)) {
                return Pacote.PRIORIDADE_EXPRESSO;
            }

            try {
                int prioridade = Integer.parseInt(entrada);
                if (prioridade >= 1 && prioridade <= 5) {
                    return prioridade;
                }
            } catch (NumberFormatException ignored) {
                // Keep loop until a valid value is provided.
            }

            System.out.println("Entrada invalida. Use EXPRESSO ou um valor entre 1 e 5.");
        }
    }

    private static void exibirPrompt(String prompt) {
        System.out.print(prompt);
        System.out.flush();
    }

    private static Scanner criarScannerInterativo() {
        try {
            return new Scanner(new FileInputStream("/dev/tty"));
        } catch (FileNotFoundException e) {
            return new Scanner(System.in);
        }
    }
}
