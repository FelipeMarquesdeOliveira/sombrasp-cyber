package br.com.fiap3esph.sombrasp.domain.cidadao;

import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;

import java.time.LocalDateTime;

/** Visão segura do cadastro: só máscaras, nunca o valor real. */
public record DadosDetalhamentoCidadao(Long id, String apelido, String bairro, String cpf, String rg,
                                       boolean possuiCondicaoSaude, String condicaoSaude, boolean aceitaAlertas,
                                       LocalDateTime criadoEm) {

    public DadosDetalhamentoCidadao(CadastroCidadao c) {
        this(c.getId(), c.getApelido(), c.getBairro(), c.getCpfMascarado(), c.getRgMascarado(),
                c.possuiCondicaoSaude(), c.possuiCondicaoSaude() ? MascaradorPii.SAUDE : null,
                c.isAceitaAlertas(), c.getCriadoEm());
    }
}
