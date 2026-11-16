package br.com.fiap3esph.sombrasp.domain.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DadosEndereco(
        @NotBlank(message = "O logradouro é obrigatório.")
        @Size(max = 100, message = "O logradouro deve ter no máximo 100 caracteres.")
        String logradouro,

        @Size(max = 10, message = "O número deve ter no máximo 10 caracteres.")
        String numero,

        @Size(max = 50, message = "O complemento deve ter no máximo 50 caracteres.")
        String complemento,

        @NotBlank(message = "O bairro é obrigatório.")
        String bairro,

        @NotBlank(message = "A cidade é obrigatória.")
        String cidade,

        @NotBlank(message = "A UF é obrigatória.")
        @Pattern(regexp = "[A-Z]{2}", message = "A UF deve ter 2 letras maiúsculas (ex.: SP).")
        String uf,

        @NotBlank(message = "O CEP é obrigatório.")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve ter o formato 00000-000.")
        String cep) {

    public DadosEndereco(Endereco endereco) {
        this(endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(),
                endereco.getBairro(), endereco.getCidade(), endereco.getUf(), endereco.getCep());
    }
}
