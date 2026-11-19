package br.com.fiap3esph.sombrasp.seguranca.evento;

import br.com.fiap3esph.sombrasp.seguranca.ia.MensagensPadrao;
import br.com.fiap3esph.sombrasp.seguranca.ia.StatusTraducao;
import br.com.fiap3esph.sombrasp.seguranca.ia.TradutorRiscoCidadao;
import br.com.fiap3esph.sombrasp.seguranca.ia.Traducao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recebe os eventos técnicos das guardas, grava na trilha de auditoria e pede a tradução
 * para a IA. A tradução roda em outra thread: a requisição do cidadão não espera o LLM.
 */
@Slf4j
@Service
public class EventoSegurancaService {
    private final EventoSegurancaRepository repository;
    private final TradutorRiscoCidadao tradutor;
    private final TaskExecutor executor;
    private final Clock relogio;

    public EventoSegurancaService(EventoSegurancaRepository repository, TradutorRiscoCidadao tradutor,
                                  @Qualifier("executorEventosSeguranca") TaskExecutor executor, Clock relogio) {
        this.repository = repository;
        this.tradutor = tradutor;
        this.executor = executor;
        this.relogio = relogio;
    }

    @PostConstruct
    void conectarPonte() {
        PonteEventosSeguranca.conectar(this::registrar);
    }

    @PreDestroy
    void desconectarPonte() {
        PonteEventosSeguranca.desconectar();
    }

    /** Registra o evento e devolve na hora o protocolo (a gravação e a tradução seguem em segundo plano). */
    public String registrar(EventoTecnico evento) {
        String protocolo = UUID.randomUUID().toString();
        executor.execute(() -> processar(protocolo, evento));
        return protocolo;
    }

    void processar(String protocolo, EventoTecnico evento) {
        try {
            StatusTraducao inicial = tradutor.habilitado() ? StatusTraducao.PENDENTE : StatusTraducao.DESATIVADA;
            EventoSeguranca salvo = repository.save(new EventoSeguranca(protocolo, evento,
                    MensagensPadrao.para(evento), inicial, LocalDateTime.now(relogio)));
            log.info("[RISCO] Evento {} ({}) registrado, protocolo {}", evento.tipo(), evento.tipo().nivel(), protocolo);
            if (!tradutor.habilitado()) {
                return;
            }
            Traducao traducao = tradutor.traduzir(evento);
            salvo.registrarTraducao(traducao, LocalDateTime.now(relogio));
            repository.save(salvo);
        } catch (RuntimeException e) {
            log.error("[RISCO] Não foi possível registrar o evento {} (protocolo {}): {}",
                    evento.tipo(), protocolo, e.getClass().getSimpleName());
        }
    }
}
