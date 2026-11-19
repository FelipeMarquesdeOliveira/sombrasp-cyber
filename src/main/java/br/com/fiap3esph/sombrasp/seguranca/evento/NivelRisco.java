package br.com.fiap3esph.sombrasp.seguranca.evento;

/** Nível do "Risco Cidadão" e a cor do farol no painel. */
public enum NivelRisco {
    BAIXO("VERDE", "Risco baixo"),
    MEDIO("AMARELO", "Atenção"),
    ALTO("VERMELHO", "Risco alto");

    private final String cor;
    private final String titulo;

    NivelRisco(String cor, String titulo) {
        this.cor = cor;
        this.titulo = titulo;
    }

    public String cor() {
        return cor;
    }

    public String titulo() {
        return titulo;
    }
}
