package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.endereco.DadosEndereco;
import br.com.fiap3esph.sombrasp.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap3esph.sombrasp.domain.ponto.DadosDetalhamentoPonto;
import br.com.fiap3esph.sombrasp.domain.ponto.DadosPontoProximo;
import br.com.fiap3esph.sombrasp.domain.ponto.TipoPonto;
import br.com.fiap3esph.sombrasp.domain.usuario.UsuarioRepository;
import br.com.fiap3esph.sombrasp.infra.security.TokenService;
import br.com.fiap3esph.sombrasp.service.PontoApoioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Camada web isolada: o serviço é simulado (Mockito), sem banco. */
@WebMvcTest(PontoApoioController.class)
@AutoConfigureMockMvc(addFilters = false)
class PontoApoioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PontoApoioService service;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String PONTO_VALIDO = """
            {"nome": "Tenda teste", "tipo": "SOMBRA_E_HIDRATACAO", "latitude": -23.55, "longitude": -46.63,
             "acessivel": true, "parceiroId": 1,
             "endereco": {"logradouro": "Praça da Sé", "bairro": "Sé", "cidade": "São Paulo", "uf": "SP", "cep": "01001-000"}}
            """;

    @Test
    void cadastrarComDadosValidosDevolve201ELocation() throws Exception {
        var endereco = new DadosEndereco("Praça da Sé", null, null, "Sé", "São Paulo", "SP", "01001-000");
        when(service.cadastrar(any())).thenReturn(new DadosDetalhamentoPonto(10L, "Tenda teste",
                TipoPonto.SOMBRA_E_HIDRATACAO, null, new BigDecimal("-23.55"), new BigDecimal("-46.63"), true,
                null, endereco, 1L, "Parceiro", true));

        mvc.perform(post("/pontos").contentType(MediaType.APPLICATION_JSON).content(PONTO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/pontos/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.parceiroNome").value("Parceiro"));
    }

    @Test
    void cadastrarComDadosInvalidosDevolve400ComOsCampos() throws Exception {
        mvc.perform(post("/pontos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "tipo": "SOMBRA", "latitude": 120, "longitude": -46.63, "acessivel": true,
                                 "parceiroId": 1, "endereco": {"logradouro": "Rua", "bairro": "Sé", "cidade": "SP",
                                 "uf": "sp", "cep": "123"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Há campos inválidos na requisição."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'latitude')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'endereco.uf')]").exists());
    }

    @Test
    void detalharPontoInexistenteDevolve404ComMensagem() throws Exception {
        when(service.detalhar(99L)).thenThrow(new RecursoNaoEncontradoException("Ponto de apoio 99 não encontrado."));

        mvc.perform(get("/pontos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Ponto de apoio 99 não encontrado."));
    }

    @Test
    void proximosRepassaCoordenadasERaioAoServico() throws Exception {
        when(service.buscarProximos(eq(-23.55), eq(-46.63), eq(0.5), isNull(), isNull()))
                .thenReturn(List.of(new DadosPontoProximo(1L, "Tenda", TipoPonto.SOMBRA_E_HIDRATACAO, "Sé",
                        "Praça da Sé", true, "08h às 18h", 0.012)));

        mvc.perform(get("/pontos/proximos").param("latitude", "-23.55").param("longitude", "-46.63")
                        .param("raioKm", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Tenda"))
                .andExpect(jsonPath("$[0].distanciaKm").value(0.012));
    }

    @Test
    void proximosSemLatitudeDevolve400() throws Exception {
        mvc.perform(get("/pontos/proximos").param("longitude", "-46.63"))
                .andExpect(status().isBadRequest());
    }
}
