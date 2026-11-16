package br.com.fiap3esph.sombrasp.domain.parceiro;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "Parceiro")
@Table(name = "parceiros")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = "id")
public class Parceiro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoParceiro tipo;

    private String email;
    private String telefone;
    private boolean ativo = true;

    public Parceiro(DadosCadastroParceiro dados) {
        this.nome = dados.nome();
        this.tipo = dados.tipo();
        this.email = dados.email();
        this.telefone = dados.telefone();
    }

    public void atualizarInformacoes(DadosAtualizacaoParceiro dados) {
        if (dados.nome() != null && !dados.nome().isBlank()) {
            this.nome = dados.nome();
        }
        if (dados.tipo() != null) {
            this.tipo = dados.tipo();
        }
        if (dados.telefone() != null && !dados.telefone().isBlank()) {
            this.telefone = dados.telefone();
        }
    }

    /** Exclusão lógica: o parceiro deixa de aparecer, mas o histórico dos pontos é preservado. */
    public void excluir() {
        this.ativo = false;
    }
}
