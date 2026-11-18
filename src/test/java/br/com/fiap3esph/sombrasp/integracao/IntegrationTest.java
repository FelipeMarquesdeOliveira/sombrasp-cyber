package br.com.fiap3esph.sombrasp.integracao;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe a aplicação completa (segurança JWT, controllers,
 * services, JPA e Flyway) sobre um H2 em memória no modo MySQL, com as MESMAS migrations
 * usadas no MySQL. Cada teste roda numa transação desfeita ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTest {
    /** Usuário gestor de demonstração criado pela migration V6 (senha documentada no README). */
    protected static final String LOGIN_GESTOR = "gestor@sombrasp.dev";
    protected static final String SENHA_GESTOR = "Gestor@SombraSP2026";

    @Autowired
    protected MockMvc mvc;

    /** Faz login pelo endpoint real e devolve "Bearer <token>". */
    protected String bearerGestor() throws Exception {
        String json = mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "%s"}
                                """.formatted(LOGIN_GESTOR, SENHA_GESTOR)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }
}
