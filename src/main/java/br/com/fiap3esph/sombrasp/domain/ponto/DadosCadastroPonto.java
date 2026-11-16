package br.com.fiap3esph.sombrasp.domain.ponto;

import br.com.fiap3esph.sombrasp.domain.endereco.DadosEndereco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DadosCadastroPonto(
        @NotBlank(message = "O nome do ponto é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotNull(message = "O tipo é obrigatório (SOMBRA, HIDRATACAO ou SOMBRA_E_HIDRATACAO).")
        TipoPonto tipo,

        @Size(max = 300, message = "A descrição deve ter no máximo 300 caracteres.")
        String descricao,

        @NotNull(message = "A latitude é obrigatória.")
        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
        BigDecimal latitude,

        @NotNull(message = "A longitude é obrigatória.")
        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
        BigDecimal longitude,

        @NotNull(message = "Informe se o ponto é acessível (true/false).")
        Boolean acessivel,

        @Size(max = 60, message = "O horário deve ter no máximo 60 caracteres.")
        String horarioFuncionamento,

        @NotNull(message = "O endereço é obrigatório.")
        @Valid
        DadosEndereco endereco,

        @NotNull(message = "O parceiro responsável é obrigatório.")
        Long parceiroId) {
}
