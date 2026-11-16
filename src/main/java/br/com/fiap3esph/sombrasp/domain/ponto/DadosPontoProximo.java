package br.com.fiap3esph.sombrasp.domain.ponto;

/** Ponto encontrado na busca por proximidade, com a distância até o cidadão. */
public record DadosPontoProximo(
        Long id,
        String nome,
        TipoPonto tipo,
        String bairro,
        String logradouro,
        boolean acessivel,
        String horarioFuncionamento,
        double distanciaKm) {
    public DadosPontoProximo(PontoApoio ponto, double distanciaKm) {
        this(ponto.getId(), ponto.getNome(), ponto.getTipo(), ponto.getEndereco().getBairro(),
                ponto.getEndereco().getLogradouro(), ponto.isAcessivel(), ponto.getHorarioFuncionamento(),
                Math.round(distanciaKm * 1000.0) / 1000.0);
    }
}
