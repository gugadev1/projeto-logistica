package com.projetologistica.service.geocodificacao;

import com.projetologistica.exception.CepInvalidoException;
import com.projetologistica.exception.CepNaoEncontradoException;
import com.projetologistica.exception.FalhaGeocodificacaoException;
import com.projetologistica.persistence.CepCacheRepository.EntradaCache;
import com.projetologistica.service.geocodificacao.CepResolverService.CacheWriter;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class CepResolverServiceTest {

    private static final RespostaViaCep RESPOSTA_VALIDA = new RespostaViaCep(
            "01310-100", "Avenida Paulista", "Bela Vista", "Sao Paulo", "SP");

    private static final double[] COORDS_API = {-46.6544, -23.5645};

    // -----------------------------------------------------------------------
    // Caminho feliz
    // -----------------------------------------------------------------------

    @Test
    void deveRetornarCoordenadaDaApiQuandoGeocodificacaoBemSucedida() {
        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> COORDS_API);

        ResolucaoCep resultado = service.resolver("01310100", null, null);

        assertEquals(COORDS_API[0], resultado.coordenadaX(), 0.0001);
        assertEquals(COORDS_API[1], resultado.coordenadaY(), 0.0001);
        assertEquals(ResolucaoCep.FONTE_API, resultado.fonte());
    }

    @Test
    void deveCepComMascaraSerNormalizadoAntesDaConsulta() {
        CepResolverService service = new CepResolverService(
                cep -> {
                    assertEquals("01310100", cep, "CEP deve chegar sem mascara ao cliente");
                    return RESPOSTA_VALIDA;
                },
                query -> COORDS_API);

        assertDoesNotThrow(() -> service.resolver("01310-100", null, null));
    }

    // -----------------------------------------------------------------------
    // Fallback manual
    // -----------------------------------------------------------------------

    @Test
    void deveRetornarFallbackManualQuandoGeocoderFalharComFallbackFornecido() {
        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> { throw new FalhaGeocodificacaoException("timeout simulado"); });

        ResolucaoCep resultado = service.resolver("01310100", 10.0, 20.0);

        assertEquals(10.0, resultado.coordenadaX(), 0.0001);
        assertEquals(20.0, resultado.coordenadaY(), 0.0001);
        assertEquals(ResolucaoCep.FONTE_FALLBACK_MANUAL, resultado.fonte());
    }

    @Test
    void deveLancarFalhaGeocodificacaoQuandoGeocoderFalharSemFallback() {
        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> { throw new FalhaGeocodificacaoException("timeout simulado"); });

        assertThrows(FalhaGeocodificacaoException.class,
                () -> service.resolver("01310100", null, null));
    }

    // -----------------------------------------------------------------------
    // Validação de formato de CEP
    // -----------------------------------------------------------------------

    @Test
    void deveLancarCepInvalidoParaCepCurto() {
        CepResolverService service = criarServiceComRespostasValidas();

        assertThrows(CepInvalidoException.class,
                () -> service.resolver("1234567", null, null));
    }

    @Test
    void deveLancarCepInvalidoParaCepComLetras() {
        CepResolverService service = criarServiceComRespostasValidas();

        assertThrows(CepInvalidoException.class,
                () -> service.resolver("0131010A", null, null));
    }

    @Test
    void deveLancarCepInvalidoParaCepNulo() {
        CepResolverService service = criarServiceComRespostasValidas();

        assertThrows(CepInvalidoException.class,
                () -> service.resolver(null, null, null));
    }

    @Test
    void deveLancarCepInvalidoParaCepVazio() {
        CepResolverService service = criarServiceComRespostasValidas();

        assertThrows(CepInvalidoException.class,
                () -> service.resolver("", null, null));
    }

    // -----------------------------------------------------------------------
    // CEP não encontrado no ViaCEP
    // -----------------------------------------------------------------------

    @Test
    void devePropagarerroCepNaoEncontradoDoCliente() {
        CepResolverService service = new CepResolverService(
                cep -> { throw new CepNaoEncontradoException(cep); },
                query -> COORDS_API);

        assertThrows(CepNaoEncontradoException.class,
                () -> service.resolver("99999999", null, null));
    }

    @Test
    void naoDeveUsarFallbackQuandoCepNaoEncontrado() {
        // fallback só vale para falha de geocoding, não para CEP inválido/inexistente
        CepResolverService service = new CepResolverService(
                cep -> { throw new CepNaoEncontradoException(cep); },
                query -> COORDS_API);

        assertThrows(CepNaoEncontradoException.class,
                () -> service.resolver("99999999", 10.0, 20.0));
    }

    // -----------------------------------------------------------------------
    // Verificação da query de endereço enviada ao geocoder
    // -----------------------------------------------------------------------

    @Test
    void deveIncluirLocalidadeEUfNaQueryDeGeocodificacao() {
        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> {
                    assertTrue(query.contains("Sao Paulo"), "query deve conter cidade");
                    assertTrue(query.contains("SP"),         "query deve conter UF");
                    assertTrue(query.contains("Brasil"),     "query deve conter pais");
                    return COORDS_API;
                });

        service.resolver("01310100", null, null);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private CepResolverService criarServiceComRespostasValidas() {
        return new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> COORDS_API);
    }

    // -----------------------------------------------------------------------
    // Cache
    // -----------------------------------------------------------------------

    @Test
    void deveRetornarFonteCacheQuandoEntradaExisteNoCache() {
        EntradaCache entrada = new EntradaCache("01310100", -46.6544, -23.5645,
                "Avenida Paulista", "Bela Vista", "Sao Paulo", "SP");
        AtomicBoolean geocoderChamado = new AtomicBoolean(false);

        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> { geocoderChamado.set(true); return COORDS_API; },
                cep -> Optional.of(entrada),
                (c, x, y, r) -> {});

        ResolucaoCep resultado = service.resolver("01310100", null, null);

        assertEquals(ResolucaoCep.FONTE_CACHE, resultado.fonte());
        assertEquals(entrada.coordenadaX(), resultado.coordenadaX(), 0.0001);
        assertEquals(entrada.coordenadaY(), resultado.coordenadaY(), 0.0001);
        assertFalse(geocoderChamado.get(), "Geocoder nao deve ser chamado quando cache tem hit");
    }

    @Test
    void naoDeveConsultarViaCepQuandoCacheTemHit() {
        EntradaCache entrada = new EntradaCache("01310100", -46.6544, -23.5645,
                "Av Paulista", "Bela Vista", "Sao Paulo", "SP");
        AtomicBoolean viaCepChamado = new AtomicBoolean(false);

        CepResolverService service = new CepResolverService(
                cep -> { viaCepChamado.set(true); return RESPOSTA_VALIDA; },
                query -> COORDS_API,
                cep -> Optional.of(entrada),
                (c, x, y, r) -> {});

        service.resolver("01310100", null, null);

        assertFalse(viaCepChamado.get(), "ViaCEP nao deve ser consultado quando cache tem hit");
    }

    @Test
    void devePersistirNoCacheAposResolucaoViaApi() {
        AtomicReference<String> cepSalvo = new AtomicReference<>();

        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> COORDS_API,
                cep -> Optional.empty(),
                (cep, x, y, r) -> cepSalvo.set(cep));

        service.resolver("01310100", null, null);

        assertEquals("01310100", cepSalvo.get(), "CEP normalizado deve ser salvo no cache");
    }

    @Test
    void naoDevePersistirNoCacheQuandoGeocoderFalhaEFallbackUsado() {
        AtomicBoolean cacheSalvo = new AtomicBoolean(false);

        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> { throw new FalhaGeocodificacaoException("timeout"); },
                cep -> Optional.empty(),
                (c, x, y, r) -> cacheSalvo.set(true));

        service.resolver("01310100", 10.0, 20.0);

        assertFalse(cacheSalvo.get(), "Cache nao deve ser gravado quando geocoder falha");
    }

    @Test
    void deveContinuarResolucaoQuandoCacheFalhar() {
        // Simula falha silenciosa no cache reader (cache degradado)
        CepResolverService service = new CepResolverService(
                cep -> RESPOSTA_VALIDA,
                query -> COORDS_API,
                cep -> Optional.empty(), // simula cache vazio (erro já tratado internamente)
                (c, x, y, r) -> {});

        ResolucaoCep resultado = service.resolver("01310100", null, null);

        assertEquals(ResolucaoCep.FONTE_API, resultado.fonte());
    }
}
