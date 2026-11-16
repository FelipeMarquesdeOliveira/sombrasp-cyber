package br.com.fiap3esph.sombrasp.domain.exception;

/** Conflito com o estado atual (ex.: alerta duplicado, e-mail já cadastrado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
