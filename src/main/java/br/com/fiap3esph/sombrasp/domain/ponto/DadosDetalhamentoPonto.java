package br.com.fiap3esph.sombrasp.domain.ponto;

import br.com.fiap3esph.sombrasp.domain.endereco.DadosEndereco;

import java.math.BigDecimal;

public record DadosDetalhamentoPonto(
        Long id,
        String nome,
        TipoPonto tipo,
        String descricao,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean acessivel,
        String horarioFuncionamento,
        DadosEndereco endereco,
        Long parceiroId,
        String parceiroNome,
        boolean ativo) {
    public DadosDetalhamentoPonto(PontoApoio ponto) {
        this(ponto.getId(), ponto.getNome(), ponto.getTipo(), ponto.getDescricao(),
                ponto.getLatitude(), ponto.getLongitude(), ponto.isAcessivel(),
                ponto.getHorarioFuncionamento(), new DadosEndereco(ponto.getEndereco()),
                ponto.getParceiro().getId(), ponto.getParceiro().getNome(), ponto.isAtivo());
    }
}
