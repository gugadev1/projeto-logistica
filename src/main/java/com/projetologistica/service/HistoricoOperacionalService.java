package com.projetologistica.service;

import com.projetologistica.persistence.HistoricoOperacionalRepository;
import com.projetologistica.service.dto.PacotePersistenciaDados;
import com.projetologistica.service.dto.RomaneioPersistenciaDados;
import com.projetologistica.service.dto.SimulacaoPersistenciaDados;

import javax.sql.DataSource;
import java.sql.Connection;

public class HistoricoOperacionalService {
    private final DataSource dataSource;
    private final HistoricoOperacionalRepository repository;

    public HistoricoOperacionalService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.repository = new HistoricoOperacionalRepository();
    }

    public void registrarSucesso(String canal, String requestJson, SimulacaoPersistenciaDados dados) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long execucaoId = repository.inserirExecucaoProcessando(connection, canal, requestJson);
                long veiculoId = repository.upsertVeiculo(connection, dados);
                long simulacaoId = repository.inserirSimulacao(connection, execucaoId, veiculoId, dados);

                for (PacotePersistenciaDados pacote : dados.pacotes()) {
                    repository.inserirPacote(connection, simulacaoId, pacote);
                }

                for (RomaneioPersistenciaDados item : dados.romaneio()) {
                    repository.inserirRomaneioItem(connection, simulacaoId, item);
                }

                repository.finalizarExecucaoSucesso(connection, execucaoId);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao persistir simulacao com rastreabilidade.", e);
        }
    }

    public void registrarFalha(String canal, String requestJson, String status, String erro) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                repository.inserirExecucaoFalha(connection, canal, requestJson, status, erro);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Falha ao persistir historico de erro: " + e.getMessage());
        }
    }
}
