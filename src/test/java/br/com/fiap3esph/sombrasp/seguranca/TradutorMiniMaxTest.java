package br.com.fiap3esph.sombrasp.seguranca;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.ia.OrigemMensagem;
import br.com.fiap3esph.sombrasp.seguranca.ia.StatusTraducao;
import br.com.fiap3esph.sombrasp.seguranca.ia.TradutorMiniMax;
import br.com.fiap3esph.sombrasp.seguranca.ia.Traducao;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** PONTO 3: integração com o LLM (prompt, resposta, falha). Sem rede: servidor simulado. */
class TradutorMiniMaxTest {
    private static final String URL = "https://api.minimax.io/v1/chat/completions";

    private final EventoTecnico evento = new EventoTecnico(TipoEventoSeguranca.VAZAMENTO_RESPOSTA_BLOQUEADO,
            "GET", "/laboratorio/ficha-saude/1", EnumSet.of(TipoPii.DADO_SAUDE, TipoPii.CPF),
            "Resposta não autenticada continha CPF 123.456.789-09 e laudo. Resposta trocada por HTTP 403.");

    private TradutorMiniMax tradutor(MockRestServiceServer[] servidor) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.minimax.io/v1");
        servidor[0] = MockRestServiceServer.bindTo(builder).build();
        return new TradutorMiniMax(builder.build(), "MiniMax-M3.1-Flash", "chave-de-teste", true);
    }

    @Test
    void enviaPromptDinamicoSemPiiEDevolveATraducaoLimpa() {
        MockRestServiceServer[] servidor = new MockRestServiceServer[1];
        TradutorMiniMax tradutor = tradutor(servidor);
        servidor[0].expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("MiniMax-M3.1-Flash"))
                .andExpect(jsonPath("$.messages[1].content", containsString("para um cidadão leigo de 60 anos, em 2 frases")))
                .andExpect(jsonPath("$.messages[1].content", containsString("***.456.***-09")))
                .andExpect(jsonPath("$.messages[1].content", not(containsString("123.456.789-09"))))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":
                        "<think>preciso ser simples</think>Tentaram acessar seu documento de saúde, mas nosso sistema bloqueou. Você não precisa fazer nada."}}]}""",
                        MediaType.APPLICATION_JSON));

        Traducao traducao = tradutor.traduzir(evento);

        servidor[0].verify();
        assertThat(traducao.origem()).isEqualTo(OrigemMensagem.IA);
        assertThat(traducao.status()).isEqualTo(StatusTraducao.CONCLUIDA);
        assertThat(traducao.mensagem())
                .isEqualTo("Tentaram acessar seu documento de saúde, mas nosso sistema bloqueou. Você não precisa fazer nada.");
    }

    @Test
    void seAIaFalharUsaAMensagemPadrao() {
        MockRestServiceServer[] servidor = new MockRestServiceServer[1];
        TradutorMiniMax tradutor = tradutor(servidor);
        servidor[0].expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Traducao traducao = tradutor.traduzir(evento);

        assertThat(traducao.origem()).isEqualTo(OrigemMensagem.PADRAO);
        assertThat(traducao.status()).isEqualTo(StatusTraducao.FALHOU);
        assertThat(traducao.mensagem()).contains("documento de saúde").contains("bloqueou");
    }

    @Test
    void semChaveATraducaoFicaDesativada() {
        TradutorMiniMax semChave = new TradutorMiniMax(RestClient.create(), "MiniMax-M3.1-Flash", "", true);
        assertThat(semChave.habilitado()).isFalse();
        assertThat(semChave.traduzir(evento).status()).isEqualTo(StatusTraducao.DESATIVADA);
    }
}
