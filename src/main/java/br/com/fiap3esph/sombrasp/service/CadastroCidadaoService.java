package br.com.fiap3esph.sombrasp.service;

import br.com.fiap3esph.sombrasp.domain.cidadao.*;
import br.com.fiap3esph.sombrasp.domain.exception.ConflitoException;
import br.com.fiap3esph.sombrasp.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap3esph.sombrasp.seguranca.pii.CifradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;
import br.com.fiap3esph.sombrasp.seguranca.taint.RegistroTaint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Cadastro do cidadão. Cada dado pessoal é mascarado, cifrado ou transformado em hash
 * ANTES de chegar ao banco (ver {@link CadastroCidadao}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CadastroCidadaoService {
    private final CadastroCidadaoRepository repository;
    private final CifradorPii cifrador;
    private final PasswordEncoder passwordEncoder;
    private final Clock relogio;

    @Transactional
    public DadosDetalhamentoCidadao cadastrar(DadosCadastroCidadao dados) {
        String cpfDigitos = dados.cpf().replaceAll("\\D", "");
        String indice = cifrador.indice(cpfDigitos);
        if (repository.existsByCpfIndice(indice)) {
            throw new ConflitoException("Este CPF já está cadastrado.");
        }
        String rg = vazioParaNulo(dados.rg());
        String saude = vazioParaNulo(dados.condicaoSaude());
        CadastroCidadao salvo = repository.save(new CadastroCidadao(
                dados.apelido().strip(),
                dados.bairro().strip(),
                MascaradorPii.cpf(cpfDigitos),
                indice,
                cifrador.cifrar(cpfDigitos),
                rg == null ? null : MascaradorPii.rg(rg),
                rg == null ? null : cifrador.cifrar(rg),
                saude == null ? null : cifrador.cifrar(saude),
                passwordEncoder.encode(dados.senha()),
                !Boolean.FALSE.equals(dados.aceitaAlertas()),
                LocalDateTime.now(relogio)));
        // log seguro: só o que já está mascarado
        log.info("Cidadão {} cadastrado: cpf={}, rg={}, saude={}", salvo.getId(), salvo.getCpfMascarado(),
                salvo.getRgMascarado(), salvo.possuiCondicaoSaude() ? MascaradorPii.SAUDE : "não informada");
        return new DadosDetalhamentoCidadao(salvo);
    }

    @Transactional(readOnly = true)
    public DadosDetalhamentoCidadao detalhar(Long id) {
        return new DadosDetalhamentoCidadao(buscar(id));
    }

    /**
     * Decifra a ficha completa. Os valores decifrados são marcados como contaminados
     * (o banco também é uma fonte de PII): se tentarem sair por log ou resposta, as
     * guardas barram.
     */
    @Transactional(readOnly = true)
    public DadosFichaSaude fichaDecifrada(Long id) {
        CadastroCidadao c = buscar(id);
        String cpf = formatarCpf(cifrador.decifrar(c.getCpfCifrado()));
        String rg = c.getRgCifrado() == null ? null : cifrador.decifrar(c.getRgCifrado());
        String saude = c.getCondicaoSaudeCifrada() == null ? null : cifrador.decifrar(c.getCondicaoSaudeCifrada());
        RegistroTaint.contaminar(TipoPii.CPF, cpf);
        RegistroTaint.contaminar(TipoPii.RG, rg);
        RegistroTaint.contaminar(TipoPii.DADO_SAUDE, saude);
        return new DadosFichaSaude(c.getId(), c.getApelido(), cpf, rg, saude);
    }

    private CadastroCidadao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cidadão " + id + " não encontrado."));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static String formatarCpf(String d) {
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
