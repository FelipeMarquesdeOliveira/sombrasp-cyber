package br.com.fiap3esph.sombrasp.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Migrações do Flyway e documentação automática (OpenAPI/Swagger). */
class InfraestruturaIntegracaoTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayAplicouAsOitoMigracoesEmOrdem() {
        List<String> versoes = jdbc.queryForList(
                // version nula = linha de "criação do schema" que o Flyway registra no H2; não é migração
                "select version from flyway_schema_history where success = true and version is not null "
                        + "order by installed_rank", String.class);
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"), versoes);
    }

    @Test
    void migracaoV5AdicionouAColunaEncerradoEm() {
        Integer colunas = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where lower(table_name) = 'alertas_calor' and lower(column_name) = 'encerrado_em'""", Integer.class);
        assertEquals(1, colunas);
    }

    @Test
    void documentacaoOpenApiPublicaTodasAsRotas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SombraSP Cyber API"))
                .andExpect(jsonPath("$.paths['/pontos/proximos']").exists())
                .andExpect(jsonPath("$.paths['/alertas/hoje']").exists())
                .andExpect(jsonPath("$.paths['/login']").exists())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"));
    }

    @Test
    void swaggerUiEstaDisponivel() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void healthCheckEhPublico() throws Exception {
        mvc.perform(get("/health-check")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
