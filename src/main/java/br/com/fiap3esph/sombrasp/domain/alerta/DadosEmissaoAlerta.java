package br.com.fiap3esph.sombrasp.domain.alerta;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosEmissaoAlerta(
        @NotNull(message = "A data de referência é obrigatória (aaaa-mm-dd).")
        LocalDate dataReferencia,

        @NotNull(message = "A temperatura máxima é obrigatória.")
        @DecimalMin(value = "-10.0", message = "Temperatura fora da faixa aceita (-10 a 60 °C).")
        @DecimalMax(value = "60.0", message = "Temperatura fora da faixa aceita (-10 a 60 °C).")
        BigDecimal temperaturaMaxima,

        @NotNull(message = "A fonte é obrigatória (MODELO_IA_INMET, ESTACAO_ESP32 ou MANUAL).")
        FonteAlerta fonte,

        @Size(max = 300, message = "A mensagem deve ter no máximo 300 caracteres.")
        String mensagem) {
}
