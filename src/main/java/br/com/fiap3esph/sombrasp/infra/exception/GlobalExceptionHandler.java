package br.com.fiap3esph.sombrasp.infra.exception;

import br.com.fiap3esph.sombrasp.domain.exception.ConflitoException;
import br.com.fiap3esph.sombrasp.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap3esph.sombrasp.domain.exception.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converte exceções em respostas JSON padronizadas e em português, sem expor stack trace.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DadosErroValidacao> tratarValidacao(MethodArgumentNotValidException e,
                                                             HttpServletRequest request) {
        List<DadosCampoInvalido> campos = e.getFieldErrors().stream().map(DadosCampoInvalido::new).toList();
        return ResponseEntity.badRequest().body(new DadosErroValidacao(400, "Dados inválidos",
                "Há campos inválidos na requisição.", request.getRequestURI(), campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DadosErro> tratarJsonInvalido(HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "JSON inválido",
                "Corpo da requisição ilegível ou com valor fora do esperado (confira datas, números e enums).", request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<DadosErro> tratarParametroInvalido(Exception e, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Parâmetro ausente ou com tipo inválido na URL.", request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<DadosErro> tratarNaoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<DadosErro> tratarRotaInexistente(HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, "Não encontrado", "Rota inexistente.", request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<DadosErro> tratarRegraDeNegocio(RegraDeNegocioException e, HttpServletRequest request) {
        return erro(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio", e.getMessage(), request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<DadosErro> tratarConflito(ConflitoException e, HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, "Conflito", e.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DadosErro> tratarIntegridade(HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, "Conflito", "A operação viola uma restrição do banco (dado duplicado ou vínculo).", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<DadosErro> tratarCredenciais(HttpServletRequest request) {
        return erro(HttpStatus.UNAUTHORIZED, "Não autenticado", "Login ou senha inválidos.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<DadosErro> tratarAutenticacao(HttpServletRequest request) {
        return erro(HttpStatus.UNAUTHORIZED, "Não autenticado", "Falha na autenticação.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DadosErro> tratarAcessoNegado(HttpServletRequest request) {
        return erro(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem permissão para esta operação.", request);
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<DadosErro> tratarBancoIndisponivel(Exception e, HttpServletRequest request) {
        log.error("Banco indisponível em {} {}", request.getMethod(), request.getRequestURI(), e);
        return erro(HttpStatus.SERVICE_UNAVAILABLE, "Banco indisponível",
                "Banco de dados indisponível no momento. Tente de novo em instantes.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DadosErro> tratarErroInesperado(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Não foi possível processar a requisição.", request);
    }

    private ResponseEntity<DadosErro> erro(HttpStatus status, String erro, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new DadosErro(status.value(), erro, mensagem, request.getRequestURI(), LocalDateTime.now()));
    }

    public record DadosErro(int status, String erro, String mensagem, String caminho, LocalDateTime timestamp) {
    }

    public record DadosErroValidacao(int status, String erro, String mensagem, String caminho,
                                     List<DadosCampoInvalido> campos) {
    }

    public record DadosCampoInvalido(String campo, String mensagem) {
        public DadosCampoInvalido(FieldError erro) {
            this(erro.getField(), erro.getDefaultMessage());
        }
    }
}
