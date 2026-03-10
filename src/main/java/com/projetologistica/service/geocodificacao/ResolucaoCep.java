package com.projetologistica.service.geocodificacao;

public record ResolucaoCep(double coordenadaX, double coordenadaY, String fonte) {
    public static final String FONTE_API = "API";
    public static final String FONTE_FALLBACK_MANUAL = "FALLBACK_MANUAL";
}
