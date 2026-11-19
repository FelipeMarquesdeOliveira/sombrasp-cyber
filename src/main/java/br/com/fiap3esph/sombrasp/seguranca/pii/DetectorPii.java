package br.com.fiap3esph.sombrasp.seguranca.pii;

import tools.jackson.databind.JsonNode;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de parsing de PII.
 *
 * <p>Usa duas estratégias, que se completam:</p>
 * <ol>
 *   <li><b>Nome do campo</b>: chaves como {@code cpf}, {@code rg}, {@code senha},
 *       {@code laudo} ou {@code condicaoSaude} marcam o valor inteiro como PII.</li>
 *   <li><b>Expressões regulares no valor</b>: acham CPF, RG, senha e CID-10 em qualquer
 *       campo, inclusive em texto livre (ex.: {@code "documento": "123.456.789-00"}).</li>
 * </ol>
 *
 * <p>É uma classe sem estado e sem dependências do Spring, para poder ser usada também
 * dentro do Logback (guarda de logs) e do {@code System.out} (guarda de console).</p>
 */
public final class DetectorPii {

    /** CPF com pontuação: o formato já basta, mesmo com dígito verificador errado. */
    static final Pattern CPF_FORMATADO = Pattern.compile("(?<![\\d.])\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}(?!\\d)");
    /** 11 dígitos seguidos: só conta como CPF se os dígitos verificadores baterem. */
    static final Pattern CPF_SO_NUMEROS = Pattern.compile("(?<!\\d)\\d{11}(?!\\d)");
    /** RG no formato de São Paulo: 12.345.678-9 (o último pode ser X). */
    static final Pattern RG_FORMATADO = Pattern.compile("(?<![\\d.])\\d{1,2}\\.\\d{3}\\.\\d{3}-[\\dXx](?![\\dA-Za-z])");
    /** "senha: abc123", "minha senha é abc123", "password=abc123" (ignora o que já está mascarado). */
    static final Pattern SENHA_NO_TEXTO = Pattern.compile(
            "(?iu)\\b(?:senha|password|pwd)\\s*(?:é|eh|:|=)\\s*(?![\\[*])([^\\s,;\"']{3,})");
    /** Código de doença: "CID I10", "CID-10: E11.9". */
    static final Pattern CID_10 = Pattern.compile("(?iu)\\bCID(?:[- ]?10)?\\s*[:\\-]?\\s*[A-TV-Z]\\d{2}(?:\\.\\d)?\\b");
    /** Palavras que indicam informação de saúde em texto livre (usadas só na entrada). */
    static final Pattern PALAVRAS_SAUDE = Pattern.compile(
            "(?iu)\\b(laudo|diagn[oó]stic\\w*|hipertens\\w*|diabet\\w*|c[aâ]ncer|hiv|depress\\w*|insulina|"
                    + "epilep\\w*|asma|cardiopat\\w*|gestante|gravidez|quimioterapia|dialise|diálise)\\b");

    private DetectorPii() {
    }

    // ------------------------------------------------------------------ entrada (source)

    /** Percorre um JSON inteiro (objetos, listas e valores) e devolve todos os dados pessoais achados. */
    public static List<AchadoPii> analisarJson(JsonNode raiz) {
        List<AchadoPii> achados = new ArrayList<>();
        percorrer(raiz, "$", null, achados);
        return achados;
    }

    /** Analisa um único campo (ex.: parâmetro da URL {@code ?cpf=...}). */
    public static List<AchadoPii> analisarCampo(String caminho, String nomeCampo, String valor) {
        List<AchadoPii> achados = new ArrayList<>();
        analisarValor(caminho, nomeCampo, valor, achados);
        return achados;
    }

    private static void percorrer(JsonNode no, String caminho, String nomeCampo, List<AchadoPii> achados) {
        if (no == null || no.isNull() || no.isMissingNode()) {
            return;
        }
        if (no.isObject()) {
            for (Map.Entry<String, JsonNode> campo : no.properties()) {
                percorrer(campo.getValue(), caminho + "." + campo.getKey(), campo.getKey(), achados);
            }
        } else if (no.isArray()) {
            int i = 0;
            for (JsonNode item : no) {
                percorrer(item, caminho + "[" + i++ + "]", nomeCampo, achados);
            }
        } else if (no.isString() || no.isNumber()) {
            analisarValor(caminho, nomeCampo, no.asString(), achados);
        }
    }

    private static void analisarValor(String caminho, String nomeCampo, String valor, List<AchadoPii> achados) {
        if (valor == null || valor.isBlank() || MascaradorPii.estaMascarado(valor)) {
            return;
        }
        TipoPii pelaChave = tipoPeloNomeDoCampo(nomeCampo);
        if (pelaChave != null) {
            achados.add(new AchadoPii(pelaChave, caminho, "nome do campo",
                    valor, MascaradorPii.mascarar(pelaChave, valor)));
            return;
        }
        List<Trecho> trechos = procurarEmTexto(valor, Modo.ENTRADA);
        boolean saudeNoTexto = trechos.stream().anyMatch(t -> t.tipo() == TipoPii.DADO_SAUDE);
        if (saudeNoTexto) {
            // texto livre que fala de saúde: o campo inteiro passa a ser dado sensível
            achados.add(new AchadoPii(TipoPii.DADO_SAUDE, caminho, "texto com informação de saúde",
                    valor, MascaradorPii.SAUDE));
        }
        for (Trecho t : trechos) {
            if (t.tipo() != TipoPii.DADO_SAUDE) {
                achados.add(new AchadoPii(t.tipo(), caminho, t.regra(), t.original(),
                        MascaradorPii.mascarar(t.tipo(), t.original())));
            }
        }
    }

    /** Classifica o nome de um campo JSON (sem acento, sem maiúsculas, só letras e números). */
    public static TipoPii tipoPeloNomeDoCampo(String nomeCampo) {
        if (nomeCampo == null) {
            return null;
        }
        String n = normalizar(nomeCampo);
        if (n.contains("senha") || n.contains("password") || n.contains("passwd") || n.equals("pwd")) {
            return TipoPii.SENHA;
        }
        if (n.contains("cpf")) {
            return TipoPii.CPF;
        }
        if (n.equals("rg") || n.contains("registrogeral") || n.equals("numerorg") || n.equals("rgnumero")) {
            return TipoPii.RG;
        }
        if (n.contains("laudo") || n.contains("diagnostic") || n.contains("saude") || n.contains("doenca")
                || n.contains("medicament") || n.contains("prontuario") || n.contains("alergia")
                || n.equals("cid") || n.equals("cid10") || n.equals("codigocid")) {
            return TipoPii.DADO_SAUDE;
        }
        return null;
    }

    // ------------------------------------------------------------------ texto livre

    /** Trecho de um texto que contém um dado pessoal. */
    public record Trecho(TipoPii tipo, int inicio, int fim, String original, String regra) {
        @Override
        public String toString() {
            return "Trecho[" + tipo + " " + inicio + ".." + fim + ", " + regra + "]";
        }
    }

    /** Quais regras de texto livre usar em cada situação. */
    public enum Modo {
        /** Entrada (dado que o cidadão enviou): todas as regras, inclusive palavras de saúde. */
        ENTRADA(true, true),
        /** Mascarar um texto (prompt da IA, detalhe técnico): números de documento, senha escrita e CID. */
        MASCARAR(true, false),
        /**
         * Saídas vigiadas (log, console, resposta HTTP): só padrões inequívocos (CPF, RG, CID).
         * Frases como "sua senha está protegida" não podem disparar bloqueio; a senha real é
         * pega pelo registro de contaminação ({@code RegistroTaint}).
         */
        SAIDA(false, false);

        final boolean senhaNoTexto;
        final boolean palavrasSaude;

        Modo(boolean senhaNoTexto, boolean palavrasSaude) {
            this.senhaNoTexto = senhaNoTexto;
            this.palavrasSaude = palavrasSaude;
        }
    }

    /** Procura dados pessoais em um texto qualquer (mensagem de log, corpo de resposta, prompt). */
    public static List<Trecho> procurarEmTexto(String texto, Modo modo) {
        List<Trecho> trechos = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            return trechos;
        }
        adicionar(trechos, CPF_FORMATADO, texto, TipoPii.CPF, "formato de CPF", false);
        Matcher m = CPF_SO_NUMEROS.matcher(texto);
        while (m.find()) {
            if (ValidadorCpf.valido(m.group())) {
                trechos.add(new Trecho(TipoPii.CPF, m.start(), m.end(), m.group(), "11 dígitos com DV de CPF"));
            }
        }
        adicionar(trechos, RG_FORMATADO, texto, TipoPii.RG, "formato de RG", false);
        if (modo.senhaNoTexto) {
            adicionar(trechos, SENHA_NO_TEXTO, texto, TipoPii.SENHA, "senha escrita no texto", true);
        }
        adicionar(trechos, CID_10, texto, TipoPii.DADO_SAUDE, "código CID-10", false);
        if (modo.palavrasSaude) {
            adicionar(trechos, PALAVRAS_SAUDE, texto, TipoPii.DADO_SAUDE, "palavra de saúde", false);
        }
        trechos.sort(Comparator.comparingInt(Trecho::inicio));
        return trechos;
    }

    private static void adicionar(List<Trecho> trechos, Pattern padrao, String texto, TipoPii tipo,
                                  String regra, boolean usarGrupo1) {
        Matcher m = padrao.matcher(texto);
        while (m.find()) {
            int ini = usarGrupo1 ? m.start(1) : m.start();
            int fim = usarGrupo1 ? m.end(1) : m.end();
            trechos.add(new Trecho(tipo, ini, fim, texto.substring(ini, fim), regra));
        }
    }

    static String normalizar(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
