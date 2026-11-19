package br.com.fiap3esph.sombrasp.domain.cidadao;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inscrição do cidadão para receber alertas de calor.
 *
 * <p>Nenhum dado pessoal é gravado em claro:</p>
 * <ul>
 *   <li>CPF: máscara para exibir, HMAC para achar duplicidade e AES-GCM para recuperar;</li>
 *   <li>RG e condição de saúde: máscara e AES-GCM;</li>
 *   <li>senha: só o hash BCrypt.</li>
 * </ul>
 */
@Entity
@Table(name = "cidadaos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CadastroCidadao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String apelido;
    private String bairro;

    @Column(name = "cpf_mascarado")
    private String cpfMascarado;
    @Column(name = "cpf_indice")
    private String cpfIndice;
    @Column(name = "cpf_cifrado")
    private String cpfCifrado;

    @Column(name = "rg_mascarado")
    private String rgMascarado;
    @Column(name = "rg_cifrado")
    private String rgCifrado;

    @Column(name = "condicao_saude_cifrada")
    private String condicaoSaudeCifrada;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(name = "aceita_alertas")
    private boolean aceitaAlertas;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public CadastroCidadao(String apelido, String bairro, String cpfMascarado, String cpfIndice, String cpfCifrado,
                           String rgMascarado, String rgCifrado, String condicaoSaudeCifrada, String senhaHash,
                           boolean aceitaAlertas, LocalDateTime criadoEm) {
        this.apelido = apelido;
        this.bairro = bairro;
        this.cpfMascarado = cpfMascarado;
        this.cpfIndice = cpfIndice;
        this.cpfCifrado = cpfCifrado;
        this.rgMascarado = rgMascarado;
        this.rgCifrado = rgCifrado;
        this.condicaoSaudeCifrada = condicaoSaudeCifrada;
        this.senhaHash = senhaHash;
        this.aceitaAlertas = aceitaAlertas;
        this.criadoEm = criadoEm;
    }

    public boolean possuiCondicaoSaude() {
        return condicaoSaudeCifrada != null;
    }
}
