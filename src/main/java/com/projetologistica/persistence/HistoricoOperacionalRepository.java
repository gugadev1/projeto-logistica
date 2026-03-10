package com.projetologistica.persistence;

import com.projetologistica.service.dto.PacotePersistenciaDados;
import com.projetologistica.service.dto.RomaneioPersistenciaDados;
import com.projetologistica.service.dto.SimulacaoPersistenciaDados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class HistoricoOperacionalRepository {

    public long inserirExecucaoProcessando(Connection connection, String canal, String requestJson) throws SQLException {
        String sql = """
                INSERT INTO operacao_execucao (canal, status, request_json, started_at)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, canal);
            statement.setString(2, "PROCESSANDO");
            statement.setString(3, requestJson);
            statement.setTimestamp(4, Timestamp.from(Instant.now()));

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public void finalizarExecucaoSucesso(Connection connection, long execucaoId) throws SQLException {
        String sql = """
                UPDATE operacao_execucao
                SET status = ?,
                    finished_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "SUCESSO");
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setLong(3, execucaoId);
            statement.executeUpdate();
        }
    }

    public long inserirExecucaoFalha(
            Connection connection,
            String canal,
            String requestJson,
            String status,
            String errorMessage
    ) throws SQLException {
        String sql = """
                INSERT INTO operacao_execucao (canal, status, request_json, error_message, started_at, finished_at)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Timestamp now = Timestamp.from(Instant.now());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, canal);
            statement.setString(2, status);
            statement.setString(3, requestJson);
            statement.setString(4, errorMessage);
            statement.setTimestamp(5, now);
            statement.setTimestamp(6, now);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public long upsertVeiculo(Connection connection, SimulacaoPersistenciaDados dados) throws SQLException {
        String sql = """
                INSERT INTO veiculo (placa, carga_max, volume_max)
                VALUES (?, ?, ?)
                ON CONFLICT (placa)
                DO UPDATE SET carga_max = EXCLUDED.carga_max,
                              volume_max = EXCLUDED.volume_max,
                              updated_at = NOW()
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dados.placa());
            statement.setDouble(2, dados.cargaMax());
            statement.setDouble(3, dados.volumeMax());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public long inserirSimulacao(
            Connection connection,
            long execucaoId,
            long veiculoId,
            SimulacaoPersistenciaDados dados
    ) throws SQLException {
        String sql = """
                INSERT INTO simulacao (
                    execucao_id,
                    veiculo_id,
                    origem_x,
                    origem_y,
                    carga_inicial,
                    carga_final,
                    volume_final,
                    ocupacao_percentual
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, execucaoId);
            statement.setLong(2, veiculoId);
            statement.setDouble(3, dados.origemX());
            statement.setDouble(4, dados.origemY());
            statement.setDouble(5, dados.cargaInicial());
            statement.setDouble(6, dados.cargaFinal());
            statement.setDouble(7, dados.volumeFinal());
            statement.setDouble(8, dados.ocupacaoPercentual());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public void inserirPacote(Connection connection, long simulacaoId, PacotePersistenciaDados pacote) throws SQLException {
        String sql = """
                INSERT INTO simulacao_pacote (
                    simulacao_id,
                    pacote_codigo,
                    peso,
                    volume,
                    coordenada_x,
                    coordenada_y,
                    prioridade,
                    status,
                    erro_validacao
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, simulacaoId);
            statement.setString(2, pacote.id());
            statement.setDouble(3, pacote.peso());
            statement.setDouble(4, pacote.volume());
            statement.setDouble(5, pacote.coordenadaX());
            statement.setDouble(6, pacote.coordenadaY());
            statement.setInt(7, pacote.prioridade());
            statement.setString(8, pacote.status());
            statement.setString(9, pacote.erroValidacao());
            statement.executeUpdate();
        }
    }

    public void inserirRomaneioItem(
            Connection connection,
            long simulacaoId,
            RomaneioPersistenciaDados item
    ) throws SQLException {
        String sql = """
                INSERT INTO romaneio_item (
                    simulacao_id,
                    pacote_codigo,
                    ordem_saida,
                    prioridade_rotulo,
                    distancia
                ) VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, simulacaoId);
            statement.setString(2, item.pacoteCodigo());
            statement.setInt(3, item.ordemSaida());
            statement.setString(4, item.prioridadeRotulo());
            statement.setDouble(5, item.distancia());
            statement.executeUpdate();
        }
    }
}
