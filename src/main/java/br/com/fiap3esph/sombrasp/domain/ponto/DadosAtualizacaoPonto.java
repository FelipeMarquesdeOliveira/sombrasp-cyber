package br.com.fiap3esph.sombrasp.domain.ponto;

import br.com.fiap3esph.sombrasp.domain.endereco.DadosEndereco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DadosAtualizacaoPonto(
        @NotNull(message = "O id do ponto é obrigatório.")
        Long id,
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,
        TipoPonto tipo,
        @Size(max = 300, message = "A descrição deve ter no máximo 300 caracteres.")
        String descricao,
        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
        BigDecimal latitude,
        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
        BigDecimal longitude,
        Boolean acessivel,
        String horarioFuncionamento,
        @Valid
        DadosEndereco endereco) {
}
