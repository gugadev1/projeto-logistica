package com.projetologistica.service;

import com.projetologistica.model.Pacote;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoteirizadorDistribuicaoTest {

    @Test
    void deveCalcularDistanciaEuclidiana() {
        RoteirizadorDistribuicao roteirizador = new RoteirizadorDistribuicao();

        double distancia = roteirizador.calcularDistanciaEuclidiana(0.0, 0.0, 3.0, 4.0);

        assertEquals(5.0, distancia, 0.0001);
    }

    @Test
    void deveOrdenarComExpressoPrimeiroDepoisPorMenorDistancia() {
        RoteirizadorDistribuicao roteirizador = new RoteirizadorDistribuicao();

        Pacote normalProximo = new Pacote("N1", 2.0, 1.0, 1.0, 1.0, 3);
        Pacote expressoLonge = new Pacote("E1", 2.0, 1.0, 10.0, 10.0, Pacote.PRIORIDADE_EXPRESSO);
        Pacote normalLonge = new Pacote("N2", 2.0, 1.0, 4.0, 4.0, 4);
        Pacote expressoProximo = new Pacote("E2", 2.0, 1.0, 2.0, 2.0, Pacote.PRIORIDADE_EXPRESSO);

        List<Pacote> ordenados = roteirizador.ordenarSaida(
                List.of(normalProximo, expressoLonge, normalLonge, expressoProximo),
                0.0,
                0.0
        );

        assertEquals("E2", ordenados.get(0).getId());
        assertEquals("E1", ordenados.get(1).getId());
        assertEquals("N1", ordenados.get(2).getId());
        assertEquals("N2", ordenados.get(3).getId());
    }
}
