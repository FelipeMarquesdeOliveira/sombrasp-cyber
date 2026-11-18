package br.com.fiap3esph.sombrasp.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PontoApoioIntegracaoTest extends IntegrationTest {

    private static final String NOVO_PONTO = """
            {"nome": "Marquise do Pátio do Colégio", "tipo": "SOMBRA", "descricao": "Marquise ampla com bancos.",
             "latitude": -23.548000, "longitude": -46.632400, "acessivel": true, "horarioFuncionamento": "Livre",
             "parceiroId": 1,
             "endereco": {"logradouro": "Pátio do Colégio", "numero": "2", "bairro": "Sé", "cidade": "São Paulo",
                          "uf": "SP", "cep": "01016-040"}}
            """;

    @Test
    void listarPontosEhPublicoETrazACargaInicial() throws Exception {
        mvc.perform(get("/pontos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].nome").isNotEmpty());
    }

    @Test
    void filtrarPorBairroETipo() throws Exception {
        mvc.perform(get("/pontos").param("bairro", "República"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("SOMBRA"));

        mvc.perform(get("/pontos").param("tipo", "HIDRATACAO"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void cadastrarSemTokenDevolve401() throws Exception {
        mvc.perform(post("/pontos").contentType(MediaType.APPLICATION_JSON).content(NOVO_PONTO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cicloCompletoCadastrarDetalharAtualizarExcluir() throws Exception {
        String token = bearerGestor();

        String criado = mvc.perform(post("/pontos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PONTO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/pontos/")))
                .andExpect(jsonPath("$.parceiroNome").value("Subprefeitura Sé (parceria proposta)"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criado, "$.id");

        mvc.perform(get("/pontos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.cep").value("01016-040"));

        mvc.perform(put("/pontos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": %d, "nome": "Marquise do Pátio do Colégio (ampliada)", "tipo": "SOMBRA_E_HIDRATACAO"}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Marquise do Pátio do Colégio (ampliada)"))
                .andExpect(jsonPath("$.tipo").value("SOMBRA_E_HIDRATACAO"));

        mvc.perform(delete("/pontos/" + id).header("Authorization", token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/pontos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastrarComParceiroInexistenteDevolve404() throws Exception {
        mvc.perform(post("/pontos").header("Authorization", bearerGestor()).contentType(MediaType.APPLICATION_JSON)
                        .content(NOVO_PONTO.replace("\"parceiroId\": 1", "\"parceiroId\": 999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Parceiro 999 não encontrado."));
    }

    @Test
    void proximosOrdenaDoMaisPertoAoMaisLonge() throws Exception {
        // Cidadão na saída do Metrô Sé, raio de 1 km
        mvc.perform(get("/pontos/proximos").param("latitude", "-23.5503").param("longitude", "-46.6339"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Tenda de hidratação da Praça da Sé"))
                .andExpect(jsonPath("$[0].distanciaKm").value(closeTo(0.0, 0.001)))
                .andExpect(jsonPath("$[1].nome").value("Loja parceira com água gratuita"))
                .andExpect(jsonPath("$[2].nome").value("Ponto de água dos ambulantes"));

        // Raio de 2 km alcança também a Praça da República
        mvc.perform(get("/pontos/proximos").param("latitude", "-23.5503").param("longitude", "-46.6339")
                        .param("raioKm", "2"))
                .andExpect(jsonPath("$.length()").value(4));

        // Quem procura SOMBRA: só a tenda (sombra e hidratação) fica a menos de 1 km
        mvc.perform(get("/pontos/proximos").param("latitude", "-23.5503").param("longitude", "-46.6339")
                        .param("tipo", "SOMBRA"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void proximosComRaioInvalidoDevolve422() throws Exception {
        mvc.perform(get("/pontos/proximos").param("latitude", "-23.55").param("longitude", "-46.63")
                        .param("raioKm", "50"))
                .andExpect(status().isUnprocessableContent());
    }
}
