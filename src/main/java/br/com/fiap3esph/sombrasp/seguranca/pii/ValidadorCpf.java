package br.com.fiap3esph.sombrasp.seguranca.pii;

/** Confere os dois dígitos verificadores de um CPF (algoritmo da Receita Federal). */
public final class ValidadorCpf {
    private ValidadorCpf() {
    }

    public static boolean valido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
