package br.com.fiap3esph.sombrasp.seguranca.pii;

import java.util.List;

/**
 * Máscaras dos dados pessoais. O resultado pode aparecer em log, tela ou banco
 * sem identificar o cidadão.
 *
 * <pre>
 *   CPF   123.456.789-00  ->  ***.456.***-00
 *   RG    12.345.678-9    ->  **.***.678-9
 *   senha qualquer        ->  [SENHA PROTEGIDA]
 *   saúde "hipertensão"   ->  [DADO DE SAÚDE PROTEGIDO]
 * </pre>
 */
public final class MascaradorPii {
    public static final String SENHA = "[SENHA PROTEGIDA]";
    public static final String SAUDE = "[DADO DE SAÚDE PROTEGIDO]";

    private MascaradorPii() {
    }

    public static String mascarar(TipoPii tipo, String valor) {
        return switch (tipo) {
            case CPF -> cpf(valor);
            case RG -> rg(valor);
            case SENHA -> SENHA;
            case DADO_SAUDE -> SAUDE;
        };
    }

    /** Mostra só o 2º bloco e os dígitos verificadores, como no exemplo do professor: {@code ***.456.***-00}. */
    public static String cpf(String valor) {
        String d = valor == null ? "" : valor.replaceAll("\\D", "");
        if (d.length() != 11) {
            return "***.***.***-**";
        }
        return "***." + d.substring(3, 6) + ".***-" + d.substring(9);
    }

    /** Mostra só os 3 últimos dígitos e o dígito final: {@code **.***.678-9}. */
    public static String rg(String valor) {
        String c = valor == null ? "" : valor.replaceAll("[^0-9Xx]", "");
        if (c.length() < 5) {
            return "**.***.***-*";
        }
        String corpo = c.substring(0, c.length() - 1);
        return "**.***." + corpo.substring(corpo.length() - 3) + "-" + c.charAt(c.length() - 1);
    }

    /** {@code true} se o valor já é uma das máscaras acima (evita mascarar duas vezes). */
    public static boolean estaMascarado(String valor) {
        if (valor == null) {
            return false;
        }
        return valor.startsWith("***.") || valor.startsWith("**.***.") || valor.equals(SENHA) || valor.equals(SAUDE);
    }

    /** Troca, dentro de um texto livre, cada dado pessoal pela sua máscara. */
    public static String mascararTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        List<DetectorPii.Trecho> trechos = DetectorPii.procurarEmTexto(texto, DetectorPii.Modo.MASCARAR);
        if (trechos.isEmpty()) {
            return texto;
        }
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        for (DetectorPii.Trecho t : trechos) {
            if (t.inicio() < cursor) {
                continue; // trechos sobrepostos: o primeiro já foi mascarado
            }
            sb.append(texto, cursor, t.inicio()).append(mascarar(t.tipo(), t.original()));
            cursor = t.fim();
        }
        return sb.append(texto.substring(cursor)).toString();
    }
}
