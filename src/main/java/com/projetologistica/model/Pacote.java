package com.projetologistica.model;

public class Pacote {
    public static final int PRIORIDADE_EXPRESSO = 1;

    private String id;
    private double peso;
    private double volume;
    private double coordenadaX;
    private double coordenadaY;
    private int prioridade;

    public Pacote(String id, double peso, double volume, double coordenadaX, double coordenadaY, int prioridade) {
        setId(id);
        setPeso(peso);
        setVolume(volume);
        setCoordenadaX(coordenadaX);
        setCoordenadaY(coordenadaY);
        setPrioridade(prioridade);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id nao pode ser vazio.");
        }
        this.id = id;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        if (peso <= 0) {
            throw new IllegalArgumentException("Peso deve ser maior que zero.");
        }
        this.peso = peso;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        if (volume <= 0) {
            throw new IllegalArgumentException("Volume deve ser maior que zero.");
        }
        this.volume = volume;
    }

    public double getCoordenadaX() {
        return coordenadaX;
    }

    public void setCoordenadaX(double coordenadaX) {
        this.coordenadaX = coordenadaX;
    }

    public double getCoordenadaY() {
        return coordenadaY;
    }

    public void setCoordenadaY(double coordenadaY) {
        this.coordenadaY = coordenadaY;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(int prioridade) {
        if (prioridade < 1 || prioridade > 5) {
            throw new IllegalArgumentException("Prioridade deve estar entre 1 e 5.");
        }
        this.prioridade = prioridade;
    }

    public boolean isExpresso() {
        return prioridade == PRIORIDADE_EXPRESSO;
    }

    @Override
    public String toString() {
        return "Pacote{" +
                "id='" + id + '\'' +
                ", peso=" + peso +
                ", volume=" + volume +
                ", coordenadaX=" + coordenadaX +
                ", coordenadaY=" + coordenadaY +
                ", prioridade=" + prioridade +
                '}';
    }
}
