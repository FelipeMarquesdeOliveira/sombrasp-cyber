package br.com.fiap3esph.sombrasp.seguranca.evento;

import br.com.fiap3esph.sombrasp.seguranca.ia.OrigemMensagem;
import br.com.fiap3esph.sombrasp.seguranca.ia.StatusTraducao;
import br.com.fiap3esph.sombrasp.seguranca.ia.Traducao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria de segurança. Guarda só o detalhe técnico mascarado (nunca PII em
 * claro) e a mensagem para o cidadão, traduzida pela IA ou padrão.
 */
@Entity
@Table(name = "eventos_seguranca")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoSeguranca {
    private static final int LIMITE_DETALHE = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String protocolo;

    @Enumerated(EnumType.STRING)
    private TipoEventoSeguranca tipo;

    @Enumerated(EnumType.STRING)
    private NivelRisco nivel;

    private String saida;
    private String metodo;
    private String caminho;
    private String modulo;

    @Column(name = "tipos_pii")
    private String tiposPii;

    @Column(name = "detalhe_tecnico")
    private String detalheTecnico;

    @Column(name = "mensagem_cidadao")
    private String mensagemCidadao;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_mensagem")
    private OrigemMensagem origemMensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_traducao")
    private StatusTraducao statusTraducao;

    @Column(name = "modelo_ia")
    private String modeloIa;

    @Column(name = "latencia_ia_ms")
    private Long latenciaIaMs;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "traduzido_em")
    private LocalDateTime traduzidoEm;

    public EventoSeguranca(String protocolo, EventoTecnico evento, String mensagemPadrao,
                           StatusTraducao status, LocalDateTime agora) {
        this.protocolo = protocolo;
        this.tipo = evento.tipo();
        this.nivel = evento.tipo().nivel();
        this.saida = evento.tipo().saida();
        this.metodo = evento.metodo();
        this.caminho = evento.caminho();
        this.modulo = evento.modulo();
        this.tiposPii = evento.tiposComoTexto();
        this.detalheTecnico = limitar(evento.detalheTecnico());
        this.mensagemCidadao = mensagemPadrao;
        this.origemMensagem = OrigemMensagem.PADRAO;
        this.statusTraducao = status;
        this.criadoEm = agora;
    }

    public void registrarTraducao(Traducao traducao, LocalDateTime agora) {
        this.mensagemCidadao = traducao.mensagem();
        this.origemMensagem = traducao.origem();
        this.statusTraducao = traducao.status();
        this.modeloIa = traducao.modelo();
        this.latenciaIaMs = traducao.latenciaMs();
        this.traduzidoEm = agora;
    }

    private static String limitar(String texto) {
        return texto == null || texto.length() <= LIMITE_DETALHE ? texto : texto.substring(0, LIMITE_DETALHE - 1) + "…";
    }
}
