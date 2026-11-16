package br.com.fiap3esph.sombrasp.domain.parceiro;

public record DadosDetalhamentoParceiro(
        Long id,
        String nome,
        TipoParceiro tipo,
        String email,
        String telefone,
        boolean ativo) {
    public DadosDetalhamentoParceiro(Parceiro parceiro) {
        this(parceiro.getId(), parceiro.getNome(), parceiro.getTipo(), parceiro.getEmail(),
                parceiro.getTelefone(), parceiro.isAtivo());
    }
}
