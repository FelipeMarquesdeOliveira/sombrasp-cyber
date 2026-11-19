package br.com.fiap3esph.sombrasp.seguranca.taint;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.evento.PonteEventosSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.pii.AchadoPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.DetectorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PONTO 1 (Parsing de PII) e início do PONTO 2 (Taint Analysis: <i>source</i>).
 *
 * <p>Middleware que intercepta toda requisição antes do Spring Security e dos controllers:
 * lê o corpo JSON (e os parâmetros da URL), procura CPF, RG, dados de saúde e senhas com o
 * {@link DetectorPii} e marca cada achado como contaminado no {@link RegistroTaint}.
 * A partir daí, as guardas de saída sabem exatamente quais valores não podem sair.</p>
 *
 * <p>O log desta etapa só mostra os valores mascarados (ex.: {@code ***.456.***-00}).</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class FiltroParsingPii extends OncePerRequestFilter {
    /** Corpos maiores que 1 MB não são analisados (a API não recebe arquivos). */
    private static final int LIMITE_CORPO = 1024 * 1024;
    public static final String ATRIBUTO_ACHADOS = "sombrasp.pii.achados";

    private final JsonMapper mapper;

    public FiltroParsingPii(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RegistroTaint.iniciar(request.getMethod(), request.getRequestURI());
        try {
            HttpServletRequest requisicao = request;
            List<AchadoPii> achados = new ArrayList<>(analisarParametros(request));
            if (temCorpo(request)) {
                byte[] corpo = request.getInputStream().readNBytes(LIMITE_CORPO + 1);
                requisicao = new RequisicaoComCorpo(request, corpo);
                if (corpo.length <= LIMITE_CORPO) {
                    achados.addAll(analisarCorpo(corpo));
                }
            }
            if (!achados.isEmpty()) {
                RegistroTaint.registrar(achados);
                request.setAttribute(ATRIBUTO_ACHADOS, List.copyOf(achados));
                registrarDeteccao(request, achados);
            }
            chain.doFilter(requisicao, response);
        } finally {
            RegistroTaint.encerrar();
        }
    }

    private List<AchadoPii> analisarParametros(HttpServletRequest request) {
        List<AchadoPii> achados = new ArrayList<>();
        for (Map.Entry<String, String[]> parametro : request.getParameterMap().entrySet()) {
            for (String valor : parametro.getValue()) {
                achados.addAll(DetectorPii.analisarCampo("?" + parametro.getKey(), parametro.getKey(), valor));
            }
        }
        return achados;
    }

    private List<AchadoPii> analisarCorpo(byte[] corpo) {
        if (corpo.length == 0) {
            return List.of();
        }
        try {
            return DetectorPii.analisarJson(mapper.readTree(corpo));
        } catch (JacksonException naoEJson) {
            // corpo que não é JSON: analisa como texto livre
            return DetectorPii.analisarCampo("$", null, new String(corpo, StandardCharsets.UTF_8));
        }
    }

    private void registrarDeteccao(HttpServletRequest request, List<AchadoPii> achados) {
        String resumo = achados.stream().map(AchadoPii::resumo).collect(Collectors.joining("; "));
        log.info("[PII] {} {} -> {} dado(s) pessoal(is) marcado(s) como contaminado(s): {}",
                request.getMethod(), request.getRequestURI(), achados.size(), resumo);
        if (request.getRequestURI().startsWith("/login")) {
            return; // senha no login é o fluxo esperado: fica marcada, mas não vira alerta no painel
        }
        Set<TipoPii> tipos = achados.stream().map(AchadoPii::tipo)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoPii.class)));
        PonteEventosSeguranca.publicar(new EventoTecnico(TipoEventoSeguranca.PII_DETECTADA,
                request.getMethod(), request.getRequestURI(), tipos,
                "PII detectada na entrada " + request.getMethod() + " " + request.getRequestURI()
                        + " e marcada como contaminada (mascarada antes de log e banco): " + resumo));
    }

    private static boolean temCorpo(HttpServletRequest request) {
        String metodo = request.getMethod();
        return "POST".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo);
    }
}
