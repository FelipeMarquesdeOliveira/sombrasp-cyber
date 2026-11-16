package br.com.fiap3esph.sombrasp.domain.ponto;

import br.com.fiap3esph.sombrasp.domain.endereco.Endereco;
import br.com.fiap3esph.sombrasp.domain.parceiro.Parceiro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity(name = "PontoApoio")
@Table(name = "pontos_apoio")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = "id")
public class PontoApoio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoPonto tipo;

    private String descricao;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean acessivel;
    private String horarioFuncionamento;

    @Embedded
    private Endereco endereco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parceiro_id")
    private Parceiro parceiro;

    private boolean ativo = true;

    public PontoApoio(DadosCadastroPonto dados, Parceiro parceiro) {
        this.nome = dados.nome();
        this.tipo = dados.tipo();
        this.descricao = dados.descricao();
        this.latitude = dados.latitude();
        this.longitude = dados.longitude();
        this.acessivel = dados.acessivel();
        this.horarioFuncionamento = dados.horarioFuncionamento();
        this.endereco = new Endereco(dados.endereco());
        this.parceiro = parceiro;
    }

    public void atualizarInformacoes(DadosAtualizacaoPonto dados) {
        if (dados.nome() != null && !dados.nome().isBlank()) {
            this.nome = dados.nome();
        }
        if (dados.tipo() != null) {
            this.tipo = dados.tipo();
        }
        if (dados.descricao() != null) {
            this.descricao = dados.descricao();
        }
        if (dados.latitude() != null) {
            this.latitude = dados.latitude();
        }
        if (dados.longitude() != null) {
            this.longitude = dados.longitude();
        }
        if (dados.acessivel() != null) {
            this.acessivel = dados.acessivel();
        }
        if (dados.horarioFuncionamento() != null) {
            this.horarioFuncionamento = dados.horarioFuncionamento();
        }
        if (dados.endereco() != null) {
            this.endereco.atualizarInformacoes(dados.endereco());
        }
    }

    /** Exclusão lógica: o ponto sai das consultas do cidadão, mas continua no banco. */
    public void excluir() {
        this.ativo = false;
    }

    /** Distância em km até uma coordenada, pela fórmula de Haversine. */
    public double distanciaKmAte(double latitudeOrigem, double longitudeOrigem) {
        return CalculadoraDistancia.haversineKm(latitudeOrigem, longitudeOrigem,
                latitude.doubleValue(), longitude.doubleValue());
    }
}
