package com.projetologistica.service.geocodificacao;

import com.projetologistica.exception.CepInvalidoException;
import com.projetologistica.exception.FalhaGeocodificacaoException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CepResolverService {

    private static final Pattern CEP_PATTERN = Pattern.compile("^\\d{8}$");

    private final CepLookupClient cepLookupClient;
    private final GeocodificacaoClient geocodificacaoClient;

    public CepResolverService(CepLookupClient cepLookupClient, GeocodificacaoClient geocodificacaoClient) {
        this.cepLookupClient = cepLookupClient;
        this.geocodificacaoClient = geocodificacaoClient;
    }

    /**
     * Resolve um CEP em coordenadas (X=lon, Y=lat).
     *
     * Comportamento:
     * - CEP malformado              → CepInvalidoException (erro de domínio, sem fallback)
     * - CEP inexistente no ViaCEP   → CepNaoEncontradoException (erro de domínio, sem fallback)
     * - Geocoder indisponível       → usa fallbackX/Y se fornecidos; caso contrário FalhaGeocodificacaoException
     */
    public ResolucaoCep resolver(String cep, Double fallbackX, Double fallbackY) {
        String cepNormalizado = normalizarCep(cep);
        validarFormato(cepNormalizado, cep);

        RespostaViaCep resposta = cepLookupClient.consultar(cepNormalizado);

        try {
            double[] coords = geocodificacaoClient.geocodificar(montarQueryEndereco(resposta));
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
