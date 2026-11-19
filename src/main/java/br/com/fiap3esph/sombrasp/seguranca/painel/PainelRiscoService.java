package br.com.fiap3esph.sombrasp.seguranca.painel;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.evento.EventoSegurancaRepository;
import br.com.fiap3esph.sombrasp.seguranca.evento.NivelRisco;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.ia.OrigemMensagem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Calcula o "Risco Cidadão": o farol mostra o nível mais grave entre os eventos da janela
 * recente (padrão: 60 minutos). A mensagem principal é a do evento mais recente desse nível.
 */
@Service
public class PainelRiscoService {
    private static final int EVENTOS_NA_TELA = 15;
    private static final String MENSAGEM_TRANQUILA =
            "Tudo tranquilo: nenhum risco aos seus dados agora. Seguimos protegendo suas informações.";

    private final EventoSegurancaRepository repository;
    private final Clock relogio;
    private final int janelaMinutos;

    public PainelRiscoService(EventoSegurancaRepository repository, Clock relogio,
                              @Value("${sombrasp.risco.janela-minutos:60}") int janelaMinutos) {
        this.repository = repository;
        this.relogio = relogio;
        this.janelaMinutos = janelaMinutos;
    }

    @Transactional(readOnly = true)
    public DadosPainelRisco montar() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<EventoSeguranca> eventos = repository.findTop100ByCriadoEmAfterOrderByCriadoEmDescIdDesc(
                agora.minusMinutes(janelaMinutos));

        DadosPainelRisco.Indicadores indicadores = new DadosPainelRisco.Indicadores(
                contar(eventos, Set.of(TipoEventoSeguranca.PII_DETECTADA, TipoEventoSeguranca.DADO_MASCARADO_NA_RESPOSTA)),
                contar(eventos, Set.of(TipoEventoSeguranca.VAZAMENTO_LOG_BLOQUEADO, TipoEventoSeguranca.VAZAMENTO_CONSOLE_BLOQUEADO)),
                contar(eventos, Set.of(TipoEventoSeguranca.VAZAMENTO_RESPOSTA_BLOQUEADO)),
                eventos.stream().filter(e -> e.getOrigemMensagem() == OrigemMensagem.IA).count(),
                eventos.size());
        List<DadosPainelRisco.DadosEventoPainel> linhas = eventos.stream().limit(EVENTOS_NA_TELA)
                .map(DadosPainelRisco.DadosEventoPainel::new).toList();

        if (eventos.isEmpty()) {
            return new DadosPainelRisco(NivelRisco.BAIXO.cor(), NivelRisco.BAIXO.name(), "Sem riscos",
                    MENSAGEM_TRANQUILA, OrigemMensagem.PADRAO.name(), null, null, null,
                    janelaMinutos, indicadores, linhas, agora);
        }
        // mais grave primeiro; no empate, o mais recente (a lista já vem do mais novo para o mais antigo)
        EventoSeguranca principal = eventos.stream()
                .max(Comparator.comparingInt((EventoSeguranca e) -> e.getNivel().ordinal()))
                .orElseThrow();
        NivelRisco nivel = principal.getNivel();
        return new DadosPainelRisco(nivel.cor(), nivel.name(), nivel.titulo(), principal.getMensagemCidadao(),
                principal.getOrigemMensagem().name(), principal.getStatusTraducao().name(), principal.getModeloIa(),
                principal.getProtocolo(), janelaMinutos, indicadores, linhas, agora);
    }

    private static long contar(List<EventoSeguranca> eventos, Set<TipoEventoSeguranca> tipos) {
        return eventos.stream().filter(e -> tipos.contains(e.getTipo())).count();
    }
}
