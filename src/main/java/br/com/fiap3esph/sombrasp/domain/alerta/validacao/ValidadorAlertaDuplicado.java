package br.com.fiap3esph.sombrasp.domain.alerta.validacao;

import br.com.fiap3esph.sombrasp.domain.alerta.AlertaCalorRepository;
import br.com.fiap3esph.sombrasp.domain.alerta.DadosEmissaoAlerta;
import br.com.fiap3esph.sombrasp.domain.exception.ConflitoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Uma mesma fonte não pode ter dois alertas ativos para o mesmo dia. */
@Component
@RequiredArgsConstructor
public class ValidadorAlertaDuplicado implements ValidadorEmissaoAlerta {
    private final AlertaCalorRepository repository;

    @Override
    public void validar(DadosEmissaoAlerta dados) {
        if (repository.existsByDataReferenciaAndFonteAndAtivoTrue(dados.dataReferencia(), dados.fonte())) {
            throw new ConflitoException("Já existe alerta ativo da fonte " + dados.fonte()
                    + " para " + dados.dataReferencia() + ". Encerre o anterior antes de emitir outro.");
        }
    }
}
