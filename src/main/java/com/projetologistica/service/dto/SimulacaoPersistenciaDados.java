package com.projetologistica.service.dto;

import java.util.List;

public record SimulacaoPersistenciaDados(
        String placa,
        double cargaMax,
        double volumeMax,
        double cargaInicial,
        double origemX,
        double origemY,
        double cargaFinal,
        double volumeFinal,
        double ocupacaoPercentual,
        List<PacotePersistenciaDados> pacotes,
        List<RomaneioPersistenciaDados> romaneio
) {
}
