package br.com.fiap3esph.sombrasp.service;

import br.com.fiap3esph.sombrasp.domain.exception.ConflitoException;
import br.com.fiap3esph.sombrasp.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap3esph.sombrasp.domain.exception.RegraDeNegocioException;
import br.com.fiap3esph.sombrasp.domain.parceiro.*;
import br.com.fiap3esph.sombrasp.domain.ponto.PontoApoioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParceiroService {
    private final ParceiroRepository repository;
    private final PontoApoioRepository pontoRepository;

    @Transactional
    public DadosDetalhamentoParceiro cadastrar(DadosCadastroParceiro dados) {
        if (repository.existsByEmailIgnoreCase(dados.email())) {
            throw new ConflitoException("Já existe parceiro com o e-mail " + dados.email() + ".");
        }
        Parceiro salvo = repository.save(new Parceiro(dados));
        return new DadosDetalhamentoParceiro(salvo);
    }

    @Transactional(readOnly = true)
    public Page<DadosListagemParceiro> listar(Pageable paginacao) {
        return repository.findAllByAtivoTrue(paginacao).map(DadosListagemParceiro::new);
    }

    @Transactional(readOnly = true)
    public DadosDetalhamentoParceiro detalhar(Long id) {
        return new DadosDetalhamentoParceiro(buscarAtivo(id));
    }

    @Transactional
    public DadosDetalhamentoParceiro atualizar(DadosAtualizacaoParceiro dados) {
        Parceiro parceiro = buscarAtivo(dados.id());
        parceiro.atualizarInformacoes(dados);
        return new DadosDetalhamentoParceiro(parceiro);
    }

    @Transactional
    public void excluir(Long id) {
        Parceiro parceiro = buscarAtivo(id);
        if (pontoRepository.countByParceiroIdAndAtivoTrue(id) > 0) {
            throw new RegraDeNegocioException(
                    "O parceiro ainda mantém pontos ativos. Transfira ou exclua os pontos antes.");
        }
        parceiro.excluir();
    }

    Parceiro buscarAtivo(Long id) {
        return repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parceiro " + id + " não encontrado."));
    }
}
