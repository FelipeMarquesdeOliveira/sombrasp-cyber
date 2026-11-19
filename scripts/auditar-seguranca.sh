#!/usr/bin/env bash
# Auditoria da camada de Cybersecurity do SombraSP.
# Roda os cenários contra a API no ar (porta 8086) e o MySQL do docker compose, e grava
# a saída real em docs/evidencias/auditoria_execucao.txt. Nenhum valor é digitado à mão.
#
# Uso: ./scripts/auditar-seguranca.sh       (com a API rodando: ./mvnw spring-boot:run)
set -euo pipefail
cd "$(dirname "$0")/.."

API="${API:-http://localhost:8086}"
SAIDA="docs/evidencias/auditoria_execucao.txt"
LOG_APP="docs/evidencias/console_aplicacao.log"
mkdir -p docs/evidencias

# Senhas do banco vêm do .env (fora do Git) e nunca são impressas.
set -a; . ./.env; set +a

# Dados FICTÍCIOS usados nos testes (CPF de exemplo com dígito verificador válido).
CPF="123.456.789-09"; RG="12.345.678-9"; SENHA="Calor@2026"; SAUDE="Hipertensão (CID I10), usa diurético"

sql() { docker exec -e MYSQL_PWD="$DB_PASSWORD" sombrasp-cyber-mysql mysql --default-character-set=utf8mb4 -usombrasp sombrasp_cyber --table -e "$1" 2>/dev/null; }
titulo() { printf '\n==========================================================================\n%s\n==========================================================================\n' "$1"; }
chamar() { # método caminho [json] [token]
  local args=(-s -w '\n<- HTTP %{http_code}\n' -X "$1" "$API$2")
  [ -n "${3:-}" ] && args+=(-H 'Content-Type: application/json' -d "$3")
  [ -n "${4:-}" ] && args+=(-H "Authorization: Bearer $4")
  echo "-> $1 $2 ${3:+(corpo abaixo)}"
  [ -n "${3:-}" ] && echo "   $3"
  curl "${args[@]}"
}

{
echo "AUDITORIA DE SEGURANÇA - SombraSP Cyber (GS 2026.2, FIAP 3ESPH)"
echo "Execução: $(date '+%d/%m/%Y %H:%M:%S %Z') | API: $API | Banco: MySQL 8.4 (container sombrasp-cyber-mysql)"
echo "Dados usados: fictícios (CPF de exemplo 123.456.789-09, RG 12.345.678-9, senha Calor@2026)."

titulo "0. Recomeço: apaga os eventos de segurança"
chamar POST /laboratorio/reiniciar

titulo "T1. PARSING DE PII: cidadão se inscreve com CPF, RG, saúde e senha"
chamar POST /cidadaos "{\"apelido\":\"Dona Maria (fictícia)\",\"bairro\":\"Sé\",\"cpf\":\"$CPF\",\"rg\":\"$RG\",\"condicaoSaude\":\"$SAUDE\",\"senha\":\"$SENHA\",\"aceitaAlertas\":true}"
echo; echo "Como ficou gravado no MySQL (tabela cidadaos):"
sql "select id, apelido, cpf_mascarado, left(cpf_indice,16) as cpf_indice_hmac, left(cpf_cifrado,28) as cpf_cifrado_aes_gcm, rg_mascarado, left(condicao_saude_cifrada,28) as saude_cifrada, left(senha_hash,20) as senha_bcrypt from cidadaos order by id desc limit 1;"

titulo "T2. PARSING DE PII no formato do enunciado: {\"documento\": \"123.456.789-00\"}"
chamar POST /laboratorio/vazamento-log '{"documento":"123.456.789-00"}'

titulo "T3. TAINT (sink LOG): código grava CPF e senha com log.info"
chamar POST /laboratorio/vazamento-log "{\"cpf\":\"$CPF\",\"senha\":\"$SENHA\"}"

titulo "T4. TAINT (sink CONSOLE): código imprime o laudo com System.out.println"
chamar POST /laboratorio/vazamento-console "{\"condicaoSaude\":\"$SAUDE\"}"

titulo "T5. TAINT (sink HTTP): ficha de saúde pedida SEM login"
chamar GET /laboratorio/ficha-saude/1

titulo "T6. TAINT (sink HTTP): a mesma ficha pedida pelo gestor autenticado (sai mascarada)"
TOKEN=$(curl -s -H 'Content-Type: application/json' "$API/login" -d '{"login":"gestor@sombrasp.dev","senha":"Gestor@SombraSP2026"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")
echo "(login do gestor feito; token omitido)"
chamar GET /laboratorio/ficha-saude/1 "" "$TOKEN"

titulo "T7. IA: aguardando a MiniMax traduzir os eventos"
for i in $(seq 1 30); do
  pend=$(sql "select count(*) as n from eventos_seguranca where status_traducao='PENDENTE';" | grep -Eo '[0-9]+' | tail -1)
  [ "${pend:-0}" = "0" ] && break; sleep 2
done
echo "Traduções pendentes: ${pend:-0}"
sql "select id, tipo, nivel, saida, modulo, origem_mensagem as origem, modelo_ia, latencia_ia_ms as ms from eventos_seguranca order by id;"
echo; echo "Detalhe técnico (mascarado) -> mensagem para o cidadão:"
sql "select id, detalhe_tecnico, mensagem_cidadao from eventos_seguranca order by id\G" | sed 's/^\*\{5,\}.*$/----/'

titulo "T8. DASHBOARD: GET /risco-cidadao (dados do painel)"
curl -s "$API/risco-cidadao" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print('farol:', d['farol'], '| nível:', d['nivel'], '| título:', d['titulo'])
print('mensagem:', d['mensagemCidadao'])
print('origem:', d['origemMensagem'], '| modelo:', d['modeloIa'])
print('indicadores:', d['indicadores'])"

titulo "T9. CONFERÊNCIA: algum valor real apareceu no log da aplicação?"
for v in "$CPF" "12345678909" "$SENHA" "$RG" "Hipertensão (CID I10)" "123.456.789-00"; do
  printf '  %-26s %s ocorrência(s)\n' "'$v'" "$(grep -c -F "$v" "$LOG_APP" || true)"
done
echo; echo "Linhas do log com as marcas [PII], [TAINT] e [IA] (como foram gravadas):"
grep -E '\[PII\]|\[TAINT\]|\[IA\]' "$LOG_APP" | sed -E 's/^([0-9T:.-]+)[^]]*\] [^ ]+ +[^:]+: /\1 /' | tail -40
} 2>&1 | tee "$SAIDA"
echo; echo "Evidência gravada em $SAIDA"
