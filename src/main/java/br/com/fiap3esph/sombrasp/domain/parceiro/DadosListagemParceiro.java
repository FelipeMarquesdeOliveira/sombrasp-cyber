package br.com.fiap3esph.sombrasp.domain.parceiro;

public record DadosListagemParceiro(
        Long id,
        String nome,
        TipoParceiro tipo,
        String email) {
    public DadosListagemParceiro(Parceiro parceiro) {
        this(parceiro.getId(), parceiro.getNome(), parceiro.getTipo(), parceiro.getEmail());
    }
}
