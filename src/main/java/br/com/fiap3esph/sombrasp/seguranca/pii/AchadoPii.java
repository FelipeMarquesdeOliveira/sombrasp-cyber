package br.com.fiap3esph.sombrasp.seguranca.pii;

import java.util.Objects;

/**
 * Um dado pessoal encontrado pelo motor de parsing: o "tag" de PII.
 *
 * <p>Guarda o valor original só em memória, durante a requisição, para que o Taint
 * Analysis saiba o que procurar nas saídas. O {@link #toString()} nunca mostra o
 * original, então o próprio achado pode ir para um log sem vazar nada.</p>
 *
 * @param tipo      categoria do dado (CPF, RG, saúde, senha)
 * @param caminho   onde o dado estava (ex.: {@code $.cpf}, {@code $.contato.documento}, {@code ?cpf})
 * @param regra     regra que detectou (ex.: "valor no formato de CPF", "nome do campo")
 * @param original  valor como chegou (fica só na memória da requisição)
 * @param mascarado valor seguro para exibir (ex.: {@code ***.456.***-00})
 */
public record AchadoPii(TipoPii tipo, String caminho, String regra, String original, String mascarado) {

    public AchadoPii {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(original, "original");
    }

    /** Resumo seguro, sem o valor original. */
    public String resumo() {
        return tipo.name() + " em " + caminho + (mascarado == null ? "" : " (" + mascarado + ")");
    }

    @Override
    public String toString() {
        return "AchadoPii[" + resumo() + ", regra=" + regra + "]";
    }
}
