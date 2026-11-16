package br.com.fiap3esph.sombrasp.domain.alerta;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Nível do alerta de calor, calculado a partir da temperatura máxima.
 * Abaixo de 31 °C não há alerta (limiar usado pelo modelo de IA&ML com dados do INMET A701).
 */
public enum NivelAlerta {
    ALERTA("Calor forte. Procure sombra e beba água a cada 20 minutos."),
    EMERGENCIA("Calor extremo. Evite o sol entre 10h e 16h, beba água sempre e procure um ponto de apoio.");

    public static final BigDecimal LIMIAR_ALERTA = new BigDecimal("31.0");
    public static final BigDecimal LIMIAR_EMERGENCIA = new BigDecimal("35.0");

    private final String mensagemPadrao;

    NivelAlerta(String mensagemPadrao) {
        this.mensagemPadrao = mensagemPadrao;
    }

    public String getMensagemPadrao() {
        return mensagemPadrao;
    }

    /** Nível correspondente à temperatura, ou vazio se ela não justifica alerta. */
    public static Optional<NivelAlerta> paraTemperatura(BigDecimal temperaturaMaxima) {
        if (temperaturaMaxima == null || temperaturaMaxima.compareTo(LIMIAR_ALERTA) < 0) {
            return Optional.empty();
        }
        return Optional.of(temperaturaMaxima.compareTo(LIMIAR_EMERGENCIA) >= 0 ? EMERGENCIA : ALERTA);
    }
}
