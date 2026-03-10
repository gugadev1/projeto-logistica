package com.projetologistica.service.dto;

public record PacotePersistenciaDados(
        String id,
        double peso,
        double volume,
        double coordenadaX,
        double coordenadaY,
        int prioridade,
        String status,
        String erroValidacao
) {
}
