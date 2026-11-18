package br.com.fiap3esph.sombrasp.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Clock;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertaCalorIntegracaoTest extends IntegrationTest {

    @Autowired
    private Clock relogio;

    private String alerta(LocalDate data, String temperatura, String fonte) {
        return """
                {"dataReferencia": "%s", "temperaturaMaxima": %s, "fonte": "%s"}
                """.formatted(data, temperatura, fonte);
    }

    @Test
    void emitirAlertaCalculaONivelPelaTemperatura() throws Exception {
        String token = bearerGestor();
        LocalDate hoje = LocalDate.now(relogio);

        mvc.perform(post("/alertas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(hoje, "36.0", "MANUAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivel").value("EMERGENCIA"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());

        mvc.perform(post("/alertas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(hoje.plusDays(2), "32.5", "MANUAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivel").value("ALERTA"));
    }

    @Test
    void temperaturaAbaixoDe31Devolve422() throws Exception {
        mvc.perform(post("/alertas").header("Authorization", bearerGestor()).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(LocalDate.now(relogio), "30.5", "MANUAL")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Temperatura máxima abaixo de 31 °C não gera alerta de calor."));
    }

    @Test
    void dataPassadaDevolve422() throws Exception {
        mvc.perform(post("/alertas").header("Authorization", bearerGestor()).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(LocalDate.now(relogio).minusDays(1), "33.0", "MANUAL")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void alertaDuplicadoDaMesmaFonteNoMesmoDiaDevolve409() throws Exception {
        String token = bearerGestor();
        LocalDate amanha = LocalDate.now(relogio).plusDays(1);
        mvc.perform(post("/alertas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(amanha, "33.0", "MANUAL")))
                .andExpect(status().isCreated());

        mvc.perform(post("/alertas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(amanha, "34.0", "MANUAL")))
                .andExpect(status().isConflict());
    }

    @Test
    void hojeDevolveOAlertaMaisGraveDoDia() throws Exception {
        mvc.perform(post("/alertas").header("Authorization", bearerGestor()).contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(LocalDate.now(relogio), "39.0", "MANUAL")))
                .andExpect(status().isCreated());

        mvc.perform(get("/alertas/hoje"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperaturaMaxima").value(39.0))
                .andExpect(jsonPath("$.nivel").value("EMERGENCIA"));
    }

    @Test
    void encerrarAlertaEhLogicoESoUmaVez() throws Exception {
        String token = bearerGestor();
        String criado = mvc.perform(post("/alertas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(LocalDate.now(relogio).plusDays(3), "33.0", "ESTACAO_ESP32")))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criado, "$.id");

        mvc.perform(delete("/alertas/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/alertas/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/alertas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.encerradoEm").isNotEmpty());
        mvc.perform(delete("/alertas/" + id).header("Authorization", token)).andExpect(status().isConflict());
    }

    @Test
    void listarAlertasEhPublico() throws Exception {
        mvc.perform(get("/alertas")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }
}
