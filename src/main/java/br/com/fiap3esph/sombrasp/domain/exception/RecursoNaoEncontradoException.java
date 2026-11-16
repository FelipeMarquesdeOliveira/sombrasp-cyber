package br.com.fiap3esph.sombrasp.domain.exception;

/** Recurso (ponto, parceiro, alerta) inexistente ou já excluído. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
