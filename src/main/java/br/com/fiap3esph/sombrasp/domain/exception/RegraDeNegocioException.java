package br.com.fiap3esph.sombrasp.domain.exception;

/** Regra de negócio violada (ex.: temperatura abaixo de 31 °C não gera alerta). Vira HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
