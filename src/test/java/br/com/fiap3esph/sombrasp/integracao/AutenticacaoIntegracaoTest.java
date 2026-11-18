package br.com.fiap3esph.sombrasp.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AutenticacaoIntegracaoTest extends IntegrationTest {

    @Test
    void loginValidoDevolveTokenJwt() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "%s"}""".formatted(LOGIN_GESTOR, SENHA_GESTOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(7200));
    }

    @Test
    void loginComSenhaErradaDevolve401() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "senha-errada"}""".formatted(LOGIN_GESTOR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Login ou senha inválidos."));
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
    }

    @Test
    void tokenInvalidoNaoAutentica() throws Exception {
        mvc.perform(post("/parceiros").header("Authorization", "Bearer token.falso.qualquer")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Não autenticado"));
    }
}
