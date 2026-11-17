package br.com.fiap3esph.sombrasp.infra.security;

import br.com.fiap3esph.sombrasp.domain.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Emite e valida o token JWT (HMAC256) usado pelos gestores. */
@Service
public class TokenService {
    private static final String ISSUER = "API SombraSP";

    private final String secret;
    private final Duration validade;
    private final Clock relogio;

    public TokenService(@Value("${api.security.token.secret}") String secret,
                        @Value("${api.security.token.validade-horas:2}") long validadeHoras,
                        Clock relogio) {
        this.secret = secret;
        this.validade = Duration.ofHours(validadeHoras);
        this.relogio = relogio;
    }

    public DadosTokenJWT gerarToken(Usuario usuario) {
        try {
            String token = JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(usuario.getLogin())
                    .withClaim("perfil", usuario.getPerfil().name())
                    .withExpiresAt(Instant.now(relogio).plus(validade))
                    .sign(Algorithm.HMAC256(secret));
            return new DadosTokenJWT(token, "Bearer", validade.toSeconds());
        } catch (JWTCreationException e) {
            throw new IllegalStateException("Erro ao gerar o token JWT.", e);
        }
    }

    /** Devolve o login (subject) de um token válido; lança exceção se for inválido ou expirado. */
    public String getSubject(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(ISSUER)
                .build()
                .verify(token)
                .getSubject();
    }

    public boolean isValido(String token) {
        try {
            getSubject(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
