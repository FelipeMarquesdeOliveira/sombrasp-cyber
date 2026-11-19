package br.com.fiap3esph.sombrasp.seguranca.taint;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.evento.PonteEventosSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.pii.DetectorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PONTO 2 (Taint Analysis): <i>sink</i> de RESPOSTA HTTP.
 *
 * <p>Inspeciona o corpo de toda resposta JSON antes de ele ser escrito. Se algum valor está
 * contaminado (veio marcado da entrada ou foi decifrado do banco) ou tem formato de documento:</p>
 * <ul>
 *   <li><b>requisição sem login</b>: a resposta inteira é trocada por um 403
 *       "Resposta bloqueada", com um protocolo para consulta no painel;</li>
 *   <li><b>gestor autenticado</b>: a resposta sai, mas com os valores mascarados.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GuardaTaintResposta implements ResponseBodyAdvice<Object> {
    private final JsonMapper mapper;

    public GuardaTaintResposta(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /** Corpo devolvido no lugar da resposta bloqueada. */
    public record DadosRespostaBloqueada(int status, String erro, String mensagem, String caminho,
                                         String protocolo, LocalDateTime timestamp) {
    }

    private record Vazamento(String caminho, TipoPii tipo) {
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        String caminho = request.getURI().getPath();
        if (body == null || body instanceof byte[] || body instanceof Resource
                || caminho.startsWith("/v3/api-docs") || caminho.startsWith("/swagger-ui")) {
            return body;
        }
        boolean corpoTexto = body instanceof String;
        JsonNode arvore;
        try {
            arvore = corpoTexto ? mapper.readTree((String) body) : mapper.valueToTree(body);
        } catch (JacksonException naoEJson) {
            return body;
        }
        List<Vazamento> vazamentos = new ArrayList<>();
        varrerEMascarar(arvore, "$", null, vazamentos);
        if (vazamentos.isEmpty()) {
            return body;
        }

        Set<TipoPii> tipos = vazamentos.stream().map(Vazamento::tipo)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoPii.class)));
        String onde = vazamentos.stream().map(v -> v.tipo() + " (" + v.caminho() + ")").collect(Collectors.joining(", "));
        String metodo = request.getMethod().name();

        if (autenticado()) {
            PonteEventosSeguranca.publicar(new EventoTecnico(TipoEventoSeguranca.DADO_MASCARADO_NA_RESPOSTA,
                    metodo, caminho, tipos, "Resposta autenticada de " + metodo + " " + caminho + " continha "
                    + onde + ". Valores mascarados antes do envio ao gestor."));
            log.info("[TAINT] Resposta de {} {} mascarada para usuário autenticado: {}", metodo, caminho, onde);
            return corpoTexto ? mapper.writeValueAsString(arvore) : arvore;
        }

        String detalhe = "Taint sink bloqueado: resposta HTTP não autenticada de " + metodo + " " + caminho
                + " continha " + onde + ". Resposta trocada por HTTP 403 (bloqueio de vazamento de PII).";
        String protocolo = PonteEventosSeguranca.publicar(new EventoTecnico(
                TipoEventoSeguranca.VAZAMENTO_RESPOSTA_BLOQUEADO, metodo, caminho, tipos, detalhe));
        log.warn("[TAINT] Resposta bloqueada: {} (protocolo {})", detalhe, protocolo);
        response.setStatusCode(HttpStatus.FORBIDDEN);
        DadosRespostaBloqueada bloqueio = new DadosRespostaBloqueada(403, "Resposta bloqueada",
                "A resposta continha dados pessoais e foi bloqueada para proteger o cidadão.",
                caminho, protocolo, LocalDateTime.now());
        return corpoTexto ? mapper.writeValueAsString(bloqueio) : bloqueio;
    }

    /** Percorre o JSON, registra cada valor que vazaria e já troca o valor pela máscara. */
    private void varrerEMascarar(JsonNode no, String caminho, String nomeCampo, List<Vazamento> vazamentos) {
        if (no instanceof ObjectNode objeto) {
            for (Map.Entry<String, JsonNode> campo : new ArrayList<>(objeto.properties())) {
                JsonNode filho = campo.getValue();
                String caminhoFilho = caminho + "." + campo.getKey();
                if (filho.isString() || filho.isNumber()) {
                    String mascarado = inspecionarValor(filho.asString(), campo.getKey(), caminhoFilho, vazamentos);
                    if (mascarado != null) {
                        objeto.put(campo.getKey(), mascarado);
                    }
                } else {
                    varrerEMascarar(filho, caminhoFilho, campo.getKey(), vazamentos);
                }
            }
        } else if (no instanceof ArrayNode lista) {
            for (int i = 0; i < lista.size(); i++) {
                JsonNode item = lista.get(i);
                String caminhoItem = caminho + "[" + i + "]";
                if (item.isString() || item.isNumber()) {
                    String mascarado = inspecionarValor(item.asString(), nomeCampo, caminhoItem, vazamentos);
                    if (mascarado != null) {
                        lista.set(i, StringNode.valueOf(mascarado));
                    }
                } else {
                    varrerEMascarar(item, caminhoItem, nomeCampo, vazamentos);
                }
            }
        }
    }

    /** Devolve o valor mascarado se ele vaza PII, ou {@code null} se pode sair como está. */
    private String inspecionarValor(String valor, String nomeCampo, String caminho, List<Vazamento> vazamentos) {
        if (valor == null || valor.isBlank() || MascaradorPii.estaMascarado(valor)) {
            return null;
        }
        TipoPii pelaChave = DetectorPii.tipoPeloNomeDoCampo(nomeCampo);
        if (pelaChave != null) {
            vazamentos.add(new Vazamento(caminho, pelaChave));
            return MascaradorPii.mascarar(pelaChave, valor);
        }
        InspetorSaida.Resultado resultado = InspetorSaida.inspecionar(valor);
        if (!resultado.vazou()) {
            return null;
        }
        resultado.tipos().forEach(tipo -> vazamentos.add(new Vazamento(caminho, tipo)));
        return resultado.textoMascarado();
    }

    private static boolean autenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
