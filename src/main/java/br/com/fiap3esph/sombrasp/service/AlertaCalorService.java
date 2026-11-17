package br.com.fiap3esph.sombrasp.service;

import br.com.fiap3esph.sombrasp.domain.alerta.*;
import br.com.fiap3esph.sombrasp.domain.alerta.validacao.ValidadorEmissaoAlerta;
import br.com.fiap3esph.sombrasp.domain.exception.ConflitoException;
import br.com.fiap3esph.sombrasp.domain.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertaCalorService {
    private final AlertaCalorRepository repository;
    private final List<ValidadorEmissaoAlerta> validadores;
    private final Clock relogio;

    /** Aplica todas as regras de emissão e calcula o nível a partir da temperatura. */
    @Transactional
    public DadosDetalhamentoAlerta emitir(DadosEmissaoAlerta dados) {
        validadores.forEach(v -> v.validar(dados));
        NivelAlerta nivel = NivelAlerta.paraTemperatura(dados.temperaturaMaxima()).orElseThrow();
        AlertaCalor salvo = repository.save(new AlertaCalor(dados, nivel, LocalDateTime.now(relogio)));
        return new DadosDetalhamentoAlerta(salvo);
    }

    @Transactional(readOnly = true)
    public Page<DadosListagemAlerta> listar(boolean somenteAtivos, Pageable paginacao) {
        Page<AlertaCalor> pagina = somenteAtivos ? repository.findAllByAtivoTrue(paginacao) : repository.findAll(paginacao);
        return pagina.map(DadosListagemAlerta::new);
    }

    @Transactional(readOnly = true)
    public DadosDetalhamentoAlerta detalhar(Long id) {
        return new DadosDetalhamentoAlerta(buscar(id));
    }

    /** Alerta ativo mais grave de hoje (o que o app mostra na tela inicial). */
    @Transactional(readOnly = true)
    public Optional<DadosDetalhamentoAlerta> alertaDeHoje() {
        return repository.findFirstByDataReferenciaAndAtivoTrueOrderByTemperaturaMaximaDesc(LocalDate.now(relogio))
                .map(DadosDetalhamentoAlerta::new);
    }

    @Transactional
    public void encerrar(Long id) {
        AlertaCalor alerta = buscar(id);
        if (!alerta.isAtivo()) {
            throw new ConflitoException("O alerta " + id + " já está encerrado.");
        }
        alerta.encerrar(LocalDateTime.now(relogio));
    }

    private AlertaCalor buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta " + id + " não encontrado."));
    }
}
