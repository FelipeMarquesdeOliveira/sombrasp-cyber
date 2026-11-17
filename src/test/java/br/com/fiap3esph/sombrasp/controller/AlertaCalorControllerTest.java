package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.alerta.DadosDetalhamentoAlerta;
import br.com.fiap3esph.sombrasp.domain.alerta.FonteAlerta;
import br.com.fiap3esph.sombrasp.domain.alerta.NivelAlerta;
import br.com.fiap3esph.sombrasp.domain.exception.RegraDeNegocioException;
import br.com.fiap3esph.sombrasp.domain.usuario.UsuarioRepository;
import br.com.fiap3esph.sombrasp.infra.security.TokenService;
import br.com.fiap3esph.sombrasp.service.AlertaCalorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertaCalorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertaCalorControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlertaCalorService service;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void emitirAlertaValidoDevolve201ComNivelCalculado() throws Exception {
        when(service.emitir(any())).thenReturn(new DadosDetalhamentoAlerta(5L, LocalDate.of(2026, 11, 19),
                new BigDecimal("36.0"), NivelAlerta.EMERGENCIA, FonteAlerta.MANUAL, "msg", true,
                LocalDateTime.of(2026, 11, 18, 15, 0), null));

        mvc.perform(post("/alertas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataReferencia": "2026-11-19", "temperaturaMaxima": 36.0, "fonte": "MANUAL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivel").value("EMERGENCIA"));
    }

    @Test
    void emitirComTemperaturaForaDaFaixaDevolve400() throws Exception {
        mvc.perform(post("/alertas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataReferencia": "2026-11-19", "temperaturaMaxima": 75.0, "fonte": "MANUAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("temperaturaMaxima"));
    }

    @Test
    void emitirComFonteInexistenteDevolve400() throws Exception {
        mvc.perform(post("/alertas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataReferencia": "2026-11-19", "temperaturaMaxima": 33.0, "fonte": "SATELITE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("JSON inválido"));
    }

    @Test
    void regraDeNegocioVira422() throws Exception {
        when(service.emitir(any())).thenThrow(
                new RegraDeNegocioException("Temperatura máxima abaixo de 31 °C não gera alerta de calor."));

        mvc.perform(post("/alertas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataReferencia": "2026-11-19", "temperaturaMaxima": 29.0, "fonte": "MANUAL"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Temperatura máxima abaixo de 31 °C não gera alerta de calor."));
    }

    @Test
    void hojeSemAlertaDevolve204() throws Exception {
        when(service.alertaDeHoje()).thenReturn(Optional.empty());

        mvc.perform(get("/alertas/hoje")).andExpect(status().isNoContent());
    }
}
