package com.projetologistica.persistence;

import com.projetologistica.service.geocodificacao.RespostaViaCep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class CepCacheRepository {

    public record EntradaCache(
            String cep,
            double coordenadaX,
            double coordenadaY,
            String logradouro,
            String bairro,
            String localidade,
            String uf) {}

    /**
     * Busca entrada de cache para o CEP normalizado (8 dígitos).
     * Retorna empty se não houver registro.
     */
    public Optional<EntradaCache> buscar(Connection connection, String cep) throws SQLException {
        String sql = """
                SELECT cep, coordenada_x, coordenada_y, logradouro, bairro, localidade, uf
                FROM cache_cep
                WHERE cep = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cep);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new EntradaCache(
                            rs.getString("cep"),
                            rs.getDouble("coordenada_x"),
                            rs.getDouble("coordenada_y"),
                            rs.getString("logradouro"),
                            rs.getString("bairro"),
                            rs.getString("localidade"),
                            rs.getString("uf")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Insere ou atualiza entrada de cache (upsert por CEP).
     */
    public void salvar(
            Connection connection,
            String cep,
            double coordenadaX,
            double coordenadaY,
            RespostaViaCep resposta
    ) throws SQLException {
        String sql = """
                INSERT INTO cache_cep (
                    cep, coordenada_x, coordenada_y,
                    logradouro, bairro, localidade, uf, atualizado_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (cep) DO UPDATE SET
                    coordenada_x  = EXCLUDED.coordenada_x,
                    coordenada_y  = EXCLUDED.coordenada_y,
                    logradouro    = EXCLUDED.logradouro,
                    bairro        = EXCLUDED.bairro,
                    localidade    = EXCLUDED.localidade,
                    uf            = EXCLUDED.uf,
                    atualizado_em = EXCLUDED.atualizado_em
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cep);
            statement.setDouble(2, coordenadaX);
            statement.setDouble(3, coordenadaY);
            statement.setString(4, resposta.logradouro());
            statement.setString(5, resposta.bairro());
            statement.setString(6, resposta.localidade());
            statement.setString(7, resposta.uf());
            statement.setTimestamp(8, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }
}
