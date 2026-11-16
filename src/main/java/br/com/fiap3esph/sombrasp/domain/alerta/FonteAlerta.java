package br.com.fiap3esph.sombrasp.domain.alerta;

/** De onde veio a temperatura que originou o alerta. */
public enum FonteAlerta {
    /** Previsão do modelo de IA&ML treinado com dados do INMET (estação A701). */
    MODELO_IA_INMET,
    /** Medição local da estação ESP32 (Physical Computing). */
    ESTACAO_ESP32,
    /** Emitido manualmente por um gestor. */
    MANUAL
}
