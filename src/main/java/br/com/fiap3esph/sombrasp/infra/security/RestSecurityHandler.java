package br.com.fiap3esph.sombrasp.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respostas JSON em português para 401 (sem token/inválido) e 403 (sem permissão). */
@Component
public class RestSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escrever(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
                "Envie um token válido no cabeçalho Authorization: Bearer <token> (obtenha em POST /login).");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(response, request, HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
                "Seu perfil não tem permissão para esta operação.");
    }

    private void escrever(HttpServletResponse response, HttpServletRequest request, int status,
                          String erro, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"status": %d, "erro": "%s", "mensagem": "%s", "caminho": "%s"}"""
                .formatted(status, erro, mensagem, request.getRequestURI()));
    }
}
