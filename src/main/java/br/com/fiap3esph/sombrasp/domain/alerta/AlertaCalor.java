package br.com.fiap3esph.sombrasp.domain.alerta;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "AlertaCalor")
@Table(name = "alertas_calor")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = "id")
public class AlertaCalor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate dataReferencia;
    private BigDecimal temperaturaMaxima;

    @Enumerated(EnumType.STRING)
    private NivelAlerta nivel;

    @Enumerated(EnumType.STRING)
    private FonteAlerta fonte;

    private String mensagem;
    private boolean ativo = true;
    private LocalDateTime emitidoEm;
    private LocalDateTime encerradoEm;

    public AlertaCalor(DadosEmissaoAlerta dados, NivelAlerta nivel, LocalDateTime agora) {
        this.dataReferencia = dados.dataReferencia();
        this.temperaturaMaxima = dados.temperaturaMaxima();
        this.nivel = nivel;
        this.fonte = dados.fonte();
        this.mensagem = (dados.mensagem() == null || dados.mensagem().isBlank())
                ? nivel.getMensagemPadrao()
                : dados.mensagem();
        this.emitidoEm = agora;
    }

    /** Encerra o alerta (exclusão lógica): deixa de valer, mas fica no histórico. */
    public void encerrar(LocalDateTime agora) {
        this.ativo = false;
        this.encerradoEm = agora;
    }
}
