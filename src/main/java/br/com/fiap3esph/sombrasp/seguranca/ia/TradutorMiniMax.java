package br.com.fiap3esph.sombrasp.seguranca.ia;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PONTO 3 (Tradução via IA): integração com o LLM da MiniMax pela API compatível com o
 * formato "chat completions" ({@code POST https://api.minimax.io/v1/chat/completions}).
 *
 * <p>Fluxo: evento técnico mascarado -> prompt dinâmico ({@link PromptRiscoCidadao}) -> LLM ->
 * resposta limpa (sem bloco de raciocínio) -> conferida de novo contra PII -> painel.
 * Qualquer falha (sem chave, timeout, erro HTTP) devolve a mensagem padrão.</p>
 */
@Slf4j
@Component
public class TradutorMiniMax implements TradutorRiscoCidadao {
    private static final Pattern BLOCO_RACIOCINIO = Pattern.compile("(?s)<think>.*?</think>");
    private static final int LIMITE_RESPOSTA = 400;
    private static final int TENTATIVAS = 2;

    private final RestClient restClient;
    private final String modelo;
    private final boolean habilitado;

    public TradutorMiniMax(@Qualifier("restClientMiniMax") RestClient restClient,
                           @Value("${sombrasp.ia.minimax.modelo}") String modelo,
                           @Value("${sombrasp.ia.minimax.chave:}") String chave,
                           @Value("${sombrasp.ia.habilitada:true}") boolean ligada) {
        this.restClient = restClient;
        this.modelo = modelo;
        this.habilitado = ligada && chave != null && !chave.isBlank();
    }

    @Override
    public boolean habilitado() {
        return habilitado;
    }

    @Override
    public Traducao traduzir(EventoTecnico evento) {
        String padrao = MensagensPadrao.para(evento);
        if (!habilitado) {
            return Traducao.padrao(padrao, StatusTraducao.DESATIVADA, null, null);
        }
        long inicio = System.nanoTime();
        try {
            Map<String, Object> corpo = Map.of(
                    "model", modelo,
                    "temperature", 0.4,
                    "max_tokens", 1500,
                    "messages", List.of(
                            Map.of("role", "system", "content", PromptRiscoCidadao.SISTEMA),
                            Map.of("role", "user", "content", PromptRiscoCidadao.usuario(evento))));
            String texto = "";
            for (int tentativa = 1; tentativa <= TENTATIVAS && texto.isBlank(); tentativa++) {
                // o modelo pensa antes de responder; se o raciocínio gastar a resposta, tenta mais uma vez
                texto = limpar(conteudo(restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .body(JsonNode.class)));
            }
            long latencia = (System.nanoTime() - inicio) / 1_000_000;
            if (texto.isBlank()) {
                log.warn("[IA] {} devolveu resposta vazia para o evento {}", modelo, evento.tipo());
                return Traducao.padrao(padrao, StatusTraducao.FALHOU, modelo, latencia);
            }
            log.info("[IA] Evento {} traduzido por {} em {} ms", evento.tipo(), modelo, latencia);
            return Traducao.daIa(texto, modelo, latencia);
        } catch (RuntimeException e) {
            long latencia = (System.nanoTime() - inicio) / 1_000_000;
            log.warn("[IA] Falha ao traduzir o evento {} com {}: {}", evento.tipo(), modelo, e.getClass().getSimpleName());
            return Traducao.padrao(padrao, StatusTraducao.FALHOU, modelo, latencia);
        }
    }

    private static String conteudo(JsonNode resposta) {
        return resposta == null ? null : resposta.path("choices").path(0).path("message").path("content").asString("");
    }

    /** Tira o raciocínio interno do modelo, espaços extras e qualquer PII que a IA tenha escrito. */
    static String limpar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpo = BLOCO_RACIOCINIO.matcher(texto).replaceAll("")
                .replace("\"", "").replaceAll("\\s+", " ").strip();
        limpo = MascaradorPii.mascararTexto(limpo);
        return limpo.length() <= LIMITE_RESPOSTA ? limpo : limpo.substring(0, LIMITE_RESPOSTA - 1) + "…";
    }
}
