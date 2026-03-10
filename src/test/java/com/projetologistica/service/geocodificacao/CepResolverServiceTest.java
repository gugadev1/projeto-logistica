package com.projetologistica.service.geocodificacao;

import com.projetologistica.exception.CepInvalidoException;
import com.projetologistica.exception.CepNaoEncontradoException;
import com.projetologistica.exception.FalhaGeocodificacaoException;
import org.junit.jupiter.api.Test;

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
}
