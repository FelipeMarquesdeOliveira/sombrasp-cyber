package br.com.fiap3esph.sombrasp.domain.alerta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosListagemAlerta(
        Long id,
        LocalDate dataReferencia,
        BigDecimal temperaturaMaxima,
        NivelAlerta nivel,
        FonteAlerta fonte,
        boolean ativo) {
    public DadosListagemAlerta(AlertaCalor alerta) {
        this(alerta.getId(), alerta.getDataReferencia(), alerta.getTemperaturaMaxima(), alerta.getNivel(),
                alerta.getFonte(), alerta.isAtivo());
    }
}
