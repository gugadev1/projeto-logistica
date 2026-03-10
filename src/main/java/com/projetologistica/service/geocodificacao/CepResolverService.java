package com.projetologistica.service.geocodificacao;

import com.projetologistica.exception.CepInvalidoException;
import com.projetologistica.exception.FalhaGeocodificacaoException;
import com.projetologistica.persistence.CepCacheRepository;
import com.projetologistica.persistence.CepCacheRepository.EntradaCache;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class CepResolverService {

    private static final Pattern CEP_PATTERN = Pattern.compile("^\\d{8}$");

    private final CepLookupClient cepLookupClient;
    private final GeocodificacaoClient geocodificacaoClient;
    private final Function<String, Optional<EntradaCache>> cacheReader;
    private final CacheWriter cacheWriter;

    /**
     * Interface funcional para escrita no cache.
     * Recebe (cep, x, y, resposta) e persiste de forma silenciosa.
     */
    @FunctionalInterface
    interface CacheWriter {
        void salvar(String cep, double x, double y, RespostaViaCep resposta);
    }

    /** Construtor sem cache – usado nos testes unitários quando cache não é relevante. */
    public CepResolverService(CepLookupClient cepLookupClient, GeocodificacaoClient geocodificacaoClient) {
        this(cepLookupClient, geocodificacaoClient, cep -> Optional.empty(), (c, x, y, r) -> {});
    }

    /** Construtor com cache em banco – usado em produção. */
    public CepResolverService(CepLookupClient cepLookupClient, GeocodificacaoClient geocodificacaoClient, DataSource dataSource) {
        this(cepLookupClient, geocodificacaoClient,
                buildCacheReader(dataSource),
                buildCacheWriter(dataSource));
    }

    /** Construtor completo – usado nos testes de integração de cache. */
    CepResolverService(
            CepLookupClient cepLookupClient,
            GeocodificacaoClient geocodificacaoClient,
            Function<String, Optional<EntradaCache>> cacheReader,
            CacheWriter cacheWriter) {
        this.cepLookupClient = cepLookupClient;
        this.geocodificacaoClient = geocodificacaoClient;
        this.cacheReader = cacheReader;
        this.cacheWriter = cacheWriter;
    }

    private static Function<String, Optional<EntradaCache>> buildCacheReader(DataSource ds) {
        CepCacheRepository repo = new CepCacheRepository();
        return cep -> {
            try (Connection conn = ds.getConnection()) {
                return repo.buscar(conn, cep);
            } catch (SQLException e) {
                System.err.println("[CepResolverService] Falha ao buscar cache de CEP: " + e.getMessage());
                return Optional.empty();
            }
        };
    }

    private static CacheWriter buildCacheWriter(DataSource ds) {
        CepCacheRepository repo = new CepCacheRepository();
        return (cep, x, y, resposta) -> {
            try (Connection conn = ds.getConnection()) {
                repo.salvar(conn, cep, x, y, resposta);
            } catch (SQLException e) {
                System.err.println("[CepResolverService] Falha ao salvar cache de CEP: " + e.getMessage());
            }
        };
    }

    /**
     * Resolve um CEP em coordenadas (X=lon, Y=lat).
     *
     * Ordem de resolução:
     * 1. Cache em banco  → ResolucaoCep com fonte CACHE
     * 2. ViaCEP + Nominatim → ResolucaoCep com fonte API  (persiste no cache)
     * 3. Fallback manual → ResolucaoCep com fonte FALLBACK_MANUAL (não persiste)
     *
     * Erros de domínio (sem fallback): CEP malformado, CEP inexistente no ViaCEP.
     */
    public ResolucaoCep resolver(String cep, Double fallbackX, Double fallbackY) {
        String cepNormalizado = normalizarCep(cep);
        validarFormato(cepNormalizado, cep);

        Optional<EntradaCache> cached = cacheReader.apply(cepNormalizado);
        if (cached.isPresent()) {
            EntradaCache entrada = cached.get();
            return new ResolucaoCep(entrada.coordenadaX(), entrada.coordenadaY(), ResolucaoCep.FONTE_CACHE);
        }

        RespostaViaCep resposta = cepLookupClient.consultar(cepNormalizado);

        try {
            double[] coords = geocodificacaoClient.geocodificar(montarQueryEndereco(resposta));
            cacheWriter.salvar(cepNormalizado, coords[0], coords[1], resposta);
            return new ResolucaoCep(coords[0], coords[1], ResolucaoCep.FONTE_API);
        } catch (FalhaGeocodificacaoException e) {
            if (fallbackX != null && fallbackY != null) {
                return new ResolucaoCep(fallbackX, fallbackY, ResolucaoCep.FONTE_FALLBACK_MANUAL);
            }
            throw new FalhaGeocodificacaoException(
                    "Nao foi possivel geocodificar o CEP " + cep
                            + ". Informe coordenadas manuais como fallback ou tente novamente.",
                    e);
        }
    }

    private String normalizarCep(String cep) {
        if (cep == null) return "";
        return cep.replaceAll("[^0-9]", "");
    }

    private void validarFormato(String cepNormalizado, String cepOriginal) {
        if (!CEP_PATTERN.matcher(cepNormalizado).matches()) {
            throw new CepInvalidoException(cepOriginal);
        }
    }

    private String montarQueryEndereco(RespostaViaCep resposta) {
        List<String> partes = new ArrayList<>();
        if (!resposta.logradouro().isBlank()) partes.add(resposta.logradouro());
        if (!resposta.bairro().isBlank())     partes.add(resposta.bairro());
        if (!resposta.localidade().isBlank()) partes.add(resposta.localidade());
        if (!resposta.uf().isBlank())         partes.add(resposta.uf());
        partes.add("Brasil");
        return String.join(", ", partes);
    }
}
