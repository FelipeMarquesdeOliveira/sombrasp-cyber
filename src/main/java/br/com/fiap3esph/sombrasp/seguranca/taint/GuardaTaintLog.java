package br.com.fiap3esph.sombrasp.seguranca.taint;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.evento.PonteEventosSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * PONTO 2 (Taint Analysis): <i>sink</i> de LOG.
 *
 * <p>Filtro do Logback que roda antes de QUALQUER linha de log da aplicação
 * ({@code log.info}, {@code log.error}, inclusive exceções). Se a mensagem final carrega um
 * valor contaminado ou um padrão de documento, a linha é descartada ({@link FilterReply#DENY})
 * e, no lugar dela, sai só um aviso com os dados mascarados. O bloqueio vira um evento de
 * segurança para o painel.</p>
 *
 * <p>Registrado em {@code logback-spring.xml}.</p>
 */
public class GuardaTaintLog extends TurboFilter {
    /** Evita recursão: o aviso de bloqueio também passa por este filtro. */
    private static final ThreadLocal<Boolean> AVISANDO = ThreadLocal.withInitial(() -> false);

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (format == null || AVISANDO.get() || level == null
                || !level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            return FilterReply.NEUTRAL;
        }
        String mensagem = montarMensagem(format, params, t);
        InspetorSaida.Resultado resultado = InspetorSaida.inspecionar(mensagem);
        if (!resultado.vazou()) {
            return FilterReply.NEUTRAL;
        }
        AVISANDO.set(true);
        try {
            String origem = RegistroTaint.atual().map(c -> " durante " + c.metodo() + " " + c.caminho()).orElse("");
            String detalhe = "Taint sink bloqueado: o logger '" + nomeCurto(logger.getName()) + "' tentou gravar "
                    + resultado.tiposPorExtenso() + " em log (nível " + level + ")" + origem
                    + ". Linha descartada; conteúdo mascarado: " + resultado.textoMascarado();
            logger.warn("[TAINT] Log bloqueado: {}", detalhe);
            PonteEventosSeguranca.publicar(new EventoTecnico(TipoEventoSeguranca.VAZAMENTO_LOG_BLOQUEADO,
                    RegistroTaint.atual().map(RegistroTaint.Contexto::metodo).orElse(null),
                    RegistroTaint.atual().map(RegistroTaint.Contexto::caminho).orElse(null),
                    resultado.tipos(), detalhe));
        } finally {
            AVISANDO.set(false);
        }
        return FilterReply.DENY;
    }

    private static String montarMensagem(String format, Object[] params, Throwable t) {
        StringBuilder sb = new StringBuilder(MessageFormatter.arrayFormat(format, params).getMessage());
        if (params != null) {
            for (Object p : params) {
                if (p instanceof Throwable erro) {
                    anexarCausas(sb, erro);
                }
            }
        }
        anexarCausas(sb, t);
        return sb.toString();
    }

    private static void anexarCausas(StringBuilder sb, Throwable erro) {
        for (int i = 0; erro != null && i < 10; i++, erro = erro.getCause()) {
            if (erro.getMessage() != null) {
                sb.append(" | ").append(erro.getMessage());
            }
        }
    }

    private static String nomeCurto(String nome) {
        int i = nome.lastIndexOf('.');
        return i < 0 ? nome : nome.substring(i + 1);
    }
}
