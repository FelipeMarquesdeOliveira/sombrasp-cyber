package br.com.fiap3esph.sombrasp.seguranca.pii;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Criptografia dos dados pessoais antes de gravar no banco.
 *
 * <ul>
 *   <li><b>AES-256-GCM</b> (com IV aleatório de 12 bytes) para os valores que precisam ser
 *       recuperados depois: CPF, RG e condição de saúde.</li>
 *   <li><b>HMAC-SHA256</b> do CPF como índice: permite saber se o CPF já foi cadastrado
 *       sem guardar o número.</li>
 * </ul>
 *
 * <p>As duas chaves são derivadas da chave mestra {@code PII_CHAVE} (variável de ambiente,
 * fora do Git). A senha do cidadão não passa por aqui: ela vira hash BCrypt.</p>
 */
@Component
public class CifradorPii {
    private static final String PREFIXO = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec chaveCifra;
    private final SecretKeySpec chaveIndice;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorPii(@Value("${sombrasp.pii.chave}") String chaveMestraBase64) {
        byte[] mestra = Base64.getDecoder().decode(chaveMestraBase64.trim());
        if (mestra.length < 32) {
            throw new IllegalStateException("PII_CHAVE precisa ter pelo menos 32 bytes (gere com: openssl rand -base64 32).");
        }
        this.chaveCifra = new SecretKeySpec(hmac(mestra, "sombrasp-pii-cifra"), "AES");
        this.chaveIndice = new SecretKeySpec(hmac(mestra, "sombrasp-pii-indice"), "HmacSHA256");
    }

    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chaveCifra, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar dado pessoal.", e);
        }
    }

    public String decifrar(String valor) {
        if (valor == null) {
            return null;
        }
        if (!valor.startsWith(PREFIXO)) {
            throw new IllegalArgumentException("Valor não está no formato cifrado esperado.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chaveCifra, new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
            byte[] claro = cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES);
            return new String(claro, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // GCM detecta qualquer alteração no texto cifrado (integridade)
            throw new IllegalStateException("Dado cifrado inválido ou adulterado.", e);
        }
    }

    /** Índice determinístico (HMAC-SHA256 em hexadecimal) de um valor já normalizado. */
    public String indice(String valorNormalizado) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chaveIndice);
            return HexFormat.of().formatHex(mac.doFinal(valorNormalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular o índice do dado pessoal.", e);
        }
    }

    private static byte[] hmac(byte[] chave, String rotulo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return mac.doFinal(rotulo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
