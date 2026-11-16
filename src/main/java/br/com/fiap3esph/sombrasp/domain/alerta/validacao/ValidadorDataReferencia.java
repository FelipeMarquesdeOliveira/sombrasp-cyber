package br.com.fiap3esph.sombrasp.domain.alerta.validacao;

import br.com.fiap3esph.sombrasp.domain.alerta.DadosEmissaoAlerta;
import br.com.fiap3esph.sombrasp.domain.exception.RegraDeNegocioException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** O alerta vale para hoje ou para os próximos 7 dias (horizonte da previsão). */
@Component
public class ValidadorDataReferencia implements ValidadorEmissaoAlerta {
    private static final int HORIZONTE_DIAS = 7;
    private final Clock relogio;

    public ValidadorDataReferencia(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public void validar(DadosEmissaoAlerta dados) {
        LocalDate hoje = LocalDate.now(relogio);
        if (dados.dataReferencia().isBefore(hoje)) {
            throw new RegraDeNegocioException("Não é possível emitir alerta para uma data que já passou.");
        }
        if (dados.dataReferencia().isAfter(hoje.plusDays(HORIZONTE_DIAS))) {
            throw new RegraDeNegocioException(
                    "O alerta só pode ser emitido para até " + HORIZONTE_DIAS + " dias à frente.");
        }
    }
}
