package br.com.fiap3esph.sombrasp.domain.ponto;

/** O que o ponto oferece ao cidadão exposto ao calor. */
public enum TipoPonto {
    SOMBRA,
    HIDRATACAO,
    SOMBRA_E_HIDRATACAO;

    /** Verdadeiro se o ponto atende a quem procura este tipo (ex.: SOMBRA_E_HIDRATACAO atende os dois). */
    public boolean atende(TipoPonto procurado) {
        return procurado == null || this == procurado || this == SOMBRA_E_HIDRATACAO;
    }
}
