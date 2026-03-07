package com.projetologistica.model;

public class Veiculo {
    private String placa;
    private double cargaMax;
    private double volumeMax;
    private double cargaAtual;

    public Veiculo(String placa, double cargaMax, double volumeMax, double cargaAtual) {
        setPlaca(placa);
        setCargaMax(cargaMax);
        setVolumeMax(volumeMax);
        setCargaAtual(cargaAtual);
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

    @Override
    public String toString() {
        return "Veiculo{" +
                "placa='" + placa + '\'' +
                ", cargaMax=" + cargaMax +
                ", volumeMax=" + volumeMax +
                ", cargaAtual=" + cargaAtual +
                '}';
    }
}
