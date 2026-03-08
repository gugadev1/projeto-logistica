package com.projetologistica.service;

import com.projetologistica.model.Pacote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class RoteirizadorDistribuicao {

    public double calcularDistanciaEuclidiana(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }

    public List<Pacote> ordenarSaida(List<Pacote> pacotes, double origemX, double origemY) {
        if (pacotes == null) {
            throw new IllegalArgumentException("Lista de pacotes nao pode ser nula.");
        }

        Comparator<Pacote> comparador = Comparator
                .comparing(Pacote::isExpresso)
                .reversed()
                .thenComparingDouble(pacote ->
                        calcularDistanciaEuclidiana(origemX, origemY, pacote.getCoordenadaX(), pacote.getCoordenadaY()))
                .thenComparing(Pacote::getId);

        PriorityQueue<Pacote> filaPrioridade = new PriorityQueue<>(comparador);
        filaPrioridade.addAll(pacotes);

        List<Pacote> ordenados = new ArrayList<>();
        while (!filaPrioridade.isEmpty()) {
            ordenados.add(filaPrioridade.poll());
        }
        return ordenados;
    }
}
