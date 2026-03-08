package com.projetologistica.model;

import com.projetologistica.exception.CapacidadeExcedidaException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Veiculo {
    private String placa;
    private double cargaMax;
    private double volumeMax;
    private double cargaAtual;
    private double volumeAtual;
    private final List<Pacote> pacotes;

    public Veiculo(String placa, double cargaMax, double volumeMax, double cargaAtual) {
        this.pacotes = new ArrayList<>();
        setPlaca(placa);
        setCargaMax(cargaMax);
        setVolumeMax(volumeMax);
        setCargaAtual(cargaAtual);
        this.volumeAtual = 0;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("Placa nao pode ser vazia.");
        }
        this.placa = placa;
    }

    public double getCargaMax() {
        return cargaMax;
    }

    public void setCargaMax(double cargaMax) {
        if (cargaMax <= 0) {
            throw new IllegalArgumentException("Carga maxima deve ser maior que zero.");
        }
        this.cargaMax = cargaMax;
    }

    public double getVolumeMax() {
        return volumeMax;
    }

    public void setVolumeMax(double volumeMax) {
        if (volumeMax <= 0) {
            throw new IllegalArgumentException("Volume maximo deve ser maior que zero.");
        }
        this.volumeMax = volumeMax;
    }

    public double getCargaAtual() {
        return cargaAtual;
    }

    public void setCargaAtual(double cargaAtual) {
        if (cargaAtual < 0) {
            throw new IllegalArgumentException("Carga atual nao pode ser negativa.");
        }
        if (cargaAtual > this.cargaMax) {
            throw new IllegalArgumentException("Carga atual nao pode exceder a carga maxima.");
        }
        this.cargaAtual = cargaAtual;
    }

    public double getVolumeAtual() {
        return volumeAtual;
    }

    public List<Pacote> getPacotes() {
        return Collections.unmodifiableList(pacotes);
    }

    public void adicionarPacote(Pacote pacote) {
        if (pacote == null) {
            throw new IllegalArgumentException("Pacote nao pode ser nulo.");
        }

        double novaCarga = cargaAtual + pacote.getPeso();
        if (novaCarga > cargaMax) {
            throw new CapacidadeExcedidaException("Capacidade de peso excedida para o veiculo.");
        }

        double novoVolume = volumeAtual + pacote.getVolume();
        if (novoVolume > volumeMax) {
            throw new CapacidadeExcedidaException("Capacidade de volume excedida para o veiculo.");
        }

        pacotes.add(pacote);
        cargaAtual = novaCarga;
        volumeAtual = novoVolume;
    }

    @Override
    public String toString() {
        return "Veiculo{" +
                "placa='" + placa + '\'' +
                ", cargaMax=" + cargaMax +
                ", volumeMax=" + volumeMax +
                ", cargaAtual=" + cargaAtual +
                ", volumeAtual=" + volumeAtual +
                '}';
    }
}
