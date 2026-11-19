package br.com.fiap3esph.sombrasp.domain.cidadao;

import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Corpo de {@code POST /cidadaos}, enviado pelo app.
 *
 * <p>O {@link #toString()} é reescrito: um {@code log.info("{}", dados)} ou
 * {@code System.out.println(dados)} esquecido no código só mostraria os valores mascarados.
 * É a primeira barreira; as guardas de saída são a segunda.</p>
 */
public record DadosCadastroCidadao(
        @NotBlank @Size(max = 60) String apelido,
        @NotBlank @Size(max = 60) String bairro,
        @NotBlank @CPF(message = "CPF inválido") String cpf,
        @Size(max = 20) String rg,
        @Size(max = 500) String condicaoSaude,
        @NotBlank @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres") String senha,
        Boolean aceitaAlertas) {

    @Override
    public String toString() {
        return "DadosCadastroCidadao[apelido=" + apelido + ", bairro=" + bairro
                + ", cpf=" + MascaradorPii.cpf(cpf)
                + ", rg=" + (rg == null ? null : MascaradorPii.rg(rg))
                + ", condicaoSaude=" + (condicaoSaude == null ? null : MascaradorPii.SAUDE)
                + ", senha=" + MascaradorPii.SENHA + ", aceitaAlertas=" + aceitaAlertas + "]";
    }
}
