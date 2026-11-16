package br.com.fiap3esph.sombrasp.domain.alerta.validacao;

import br.com.fiap3esph.sombrasp.domain.alerta.DadosEmissaoAlerta;
import br.com.fiap3esph.sombrasp.domain.alerta.NivelAlerta;
import br.com.fiap3esph.sombrasp.domain.exception.RegraDeNegocioException;
import org.springframework.stereotype.Component;

/** Só há alerta de calor quando a máxima chega a 31 °C. */
@Component
public class ValidadorTemperaturaDeAlerta implements ValidadorEmissaoAlerta {
    @Override
    public void validar(DadosEmissaoAlerta dados) {
        if (NivelAlerta.paraTemperatura(dados.temperaturaMaxima()).isEmpty()) {
            throw new RegraDeNegocioException(
                    "Temperatura máxima abaixo de 31 °C não gera alerta de calor.");
        }
    }
}
