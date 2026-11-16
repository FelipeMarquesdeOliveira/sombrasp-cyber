package br.com.fiap3esph.sombrasp.domain.ponto;

import java.math.BigDecimal;

public record DadosListagemPonto(
        Long id,
        String nome,
        TipoPonto tipo,
        String bairro,
        boolean acessivel,
        BigDecimal latitude,
        BigDecimal longitude) {
    public DadosListagemPonto(PontoApoio ponto) {
        this(ponto.getId(), ponto.getNome(), ponto.getTipo(), ponto.getEndereco().getBairro(),
                ponto.isAcessivel(), ponto.getLatitude(), ponto.getLongitude());
    }
}
