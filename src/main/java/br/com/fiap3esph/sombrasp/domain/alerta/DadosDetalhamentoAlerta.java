package br.com.fiap3esph.sombrasp.domain.alerta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DadosDetalhamentoAlerta(
        Long id,
        LocalDate dataReferencia,
        BigDecimal temperaturaMaxima,
        NivelAlerta nivel,
        FonteAlerta fonte,
        String mensagem,
        boolean ativo,
        LocalDateTime emitidoEm,
        LocalDateTime encerradoEm) {
    public DadosDetalhamentoAlerta(AlertaCalor alerta) {
        this(alerta.getId(), alerta.getDataReferencia(), alerta.getTemperaturaMaxima(), alerta.getNivel(),
                alerta.getFonte(), alerta.getMensagem(), alerta.isAtivo(), alerta.getEmitidoEm(),
                alerta.getEncerradoEm());
    }
}
