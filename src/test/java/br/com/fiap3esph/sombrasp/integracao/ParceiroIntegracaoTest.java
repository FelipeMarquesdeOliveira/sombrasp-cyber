package br.com.fiap3esph.sombrasp.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParceiroIntegracaoTest extends IntegrationTest {

    @Test
    void cadastrarParceiroEListar() throws Exception {
        mvc.perform(post("/parceiros").header("Authorization", bearerGestor()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Mercado Municipal (parceria fictícia)", "tipo": "COMERCIO",
                                 "email": "mercadao@parceiro-ficticio.sombrasp.dev", "telefone": "1133334444"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true));

        mvc.perform(get("/parceiros")).andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    void emailDuplicadoDevolve409() throws Exception {
        mvc.perform(post("/parceiros").header("Authorization", bearerGestor()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outro", "tipo": "ONG", "email": "loja@parceiro-ficticio.sombrasp.dev"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void naoExcluiParceiroQueAindaMantemPontos() throws Exception {
        mvc.perform(delete("/parceiros/1").header("Authorization", bearerGestor()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(
                        "O parceiro ainda mantém pontos ativos. Transfira ou exclua os pontos antes."));
    }
}
