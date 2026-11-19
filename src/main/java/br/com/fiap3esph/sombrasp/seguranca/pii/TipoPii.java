package br.com.fiap3esph.sombrasp.seguranca.pii;

/**
 * Categorias de dado pessoal (PII) que o motor de parsing reconhece.
 * São os dados citados na LGPD como identificadores fortes (CPF, RG), dados
 * pessoais sensíveis (saúde, art. 5º, II) e credenciais (senha).
 */
public enum TipoPii {
    CPF("CPF"),
    RG("RG"),
    DADO_SAUDE("dado de saúde"),
    SENHA("senha");

    private final String descricao;

    TipoPii(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
