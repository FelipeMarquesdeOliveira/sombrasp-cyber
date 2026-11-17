package br.com.fiap3esph.sombrasp.infra.security;

public record DadosTokenJWT(String token, String tipo, long expiraEmSegundos) {
}
