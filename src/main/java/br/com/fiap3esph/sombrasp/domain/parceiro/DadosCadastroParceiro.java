package br.com.fiap3esph.sombrasp.domain.parceiro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DadosCadastroParceiro(
        @NotBlank(message = "O nome do parceiro é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotNull(message = "O tipo do parceiro é obrigatório (PODER_PUBLICO, COMERCIO ou ONG).")
        TipoParceiro tipo,

        @NotBlank(message = "O e-mail de contato é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter 10 ou 11 dígitos, só números.")
        String telefone) {
}
