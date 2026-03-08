package com.projetologistica.model;

import com.projetologistica.exception.CapacidadeExcedidaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VeiculoTest {

    @Test
    void deveBloquearQuandoExcederCapacidadePeso() {
        Veiculo veiculo = new Veiculo("ABC-1234", 100.0, 10.0, 90.0);
        Pacote pacote = new Pacote("P1", 15.0, 1.0, 0.0, 0.0, 3);

        assertThrows(CapacidadeExcedidaException.class, () -> veiculo.adicionarPacote(pacote));
    }

    @Test
    void deveBloquearQuandoExcederCapacidadeVolume() {
        Veiculo veiculo = new Veiculo("DEF-5678", 100.0, 10.0, 0.0);
        veiculo.adicionarPacote(new Pacote("P1", 10.0, 8.0, 0.0, 0.0, 2));

        Pacote pacoteExcedente = new Pacote("P2", 5.0, 3.0, 1.0, 1.0, 4);

        assertThrows(CapacidadeExcedidaException.class, () -> veiculo.adicionarPacote(pacoteExcedente));
    }

    @Test
    void devePermitirCarregamentoQuandoDentroDoLimite() {
        Veiculo veiculo = new Veiculo("GHI-9012", 100.0, 10.0, 95.0);
        Pacote pacote = new Pacote("P1", 5.0, 10.0, 0.0, 0.0, 1);

        veiculo.adicionarPacote(pacote);

        assertEquals(100.0, veiculo.getCargaAtual());
        assertEquals(10.0, veiculo.getVolumeAtual());
        assertEquals(1, veiculo.getPacotes().size());
    }
}
