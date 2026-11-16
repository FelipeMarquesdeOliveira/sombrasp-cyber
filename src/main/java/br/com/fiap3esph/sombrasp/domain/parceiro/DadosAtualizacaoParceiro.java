package br.com.fiap3esph.sombrasp.domain.parceiro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DadosAtualizacaoParceiro(
        @NotNull(message = "O id do parceiro é obrigatório.")
        Long id,
        String nome,
        TipoParceiro tipo,
        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter 10 ou 11 dígitos, só números.")
        String telefone) {
}
