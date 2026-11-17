package br.com.fiap3esph.sombrasp.service;

import br.com.fiap3esph.sombrasp.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap3esph.sombrasp.domain.exception.RegraDeNegocioException;
import br.com.fiap3esph.sombrasp.domain.parceiro.Parceiro;
import br.com.fiap3esph.sombrasp.domain.ponto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PontoApoioService {
    /** Raio máximo da busca por proximidade (km). */
    static final double RAIO_MAXIMO_KM = 10.0;

    private final PontoApoioRepository repository;
    private final ParceiroService parceiroService;

    @Transactional
    public DadosDetalhamentoPonto cadastrar(DadosCadastroPonto dados) {
        Parceiro parceiro = parceiroService.buscarAtivo(dados.parceiroId());
        PontoApoio salvo = repository.save(new PontoApoio(dados, parceiro));
        return new DadosDetalhamentoPonto(salvo);
    }

    @Transactional(readOnly = true)
    public Page<DadosListagemPonto> listar(TipoPonto tipo, String bairro, Boolean acessivel, Pageable paginacao) {
        return repository.buscarAtivos(tipo, bairro, acessivel, paginacao).map(DadosListagemPonto::new);
    }

    @Transactional(readOnly = true)
    public DadosDetalhamentoPonto detalhar(Long id) {
        return new DadosDetalhamentoPonto(buscarAtivo(id));
    }

    @Transactional
    public DadosDetalhamentoPonto atualizar(DadosAtualizacaoPonto dados) {
        PontoApoio ponto = buscarAtivo(dados.id());
        ponto.atualizarInformacoes(dados);
        return new DadosDetalhamentoPonto(ponto);
    }

    @Transactional
    public void excluir(Long id) {
        buscarAtivo(id).excluir();
    }

    /**
     * Pontos ativos num raio (km) a partir da posição do cidadão, do mais perto para o mais longe.
     * É a consulta que o app usa no dia de alerta para indicar sombra e água por perto.
     */
    @Transactional(readOnly = true)
    public List<DadosPontoProximo> buscarProximos(double latitude, double longitude, double raioKm,
                                                  TipoPonto tipo, Boolean acessivel) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new RegraDeNegocioException("Coordenadas inválidas: latitude de -90 a 90 e longitude de -180 a 180.");
        }
        if (raioKm <= 0 || raioKm > RAIO_MAXIMO_KM) {
            throw new RegraDeNegocioException("O raio deve ser maior que 0 e no máximo " + RAIO_MAXIMO_KM + " km.");
        }
        return repository.findAllByAtivoTrue().stream()
                .filter(p -> p.getTipo().atende(tipo))
                .filter(p -> acessivel == null || p.isAcessivel() == acessivel)
                .map(p -> new DadosPontoProximo(p, p.distanciaKmAte(latitude, longitude)))
                .filter(p -> p.distanciaKm() <= raioKm)
                .sorted(Comparator.comparingDouble(DadosPontoProximo::distanciaKm))
                .toList();
    }

    private PontoApoio buscarAtivo(Long id) {
        return repository.buscarAtivoPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto de apoio " + id + " não encontrado."));
    }
}
