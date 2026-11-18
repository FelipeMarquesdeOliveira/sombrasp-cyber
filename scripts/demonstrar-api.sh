#!/usr/bin/env bash
# SombraSP (SOA) | Roteiro de demonstração contra a API real (MySQL).
# Pré-requisito: MySQL (docker compose) e API rodando em http://localhost:8085.
# Uso: ./scripts/demonstrar-api.sh            -> grava docs/evidencias/roteiro_api_mysql.txt
#      ./scripts/demonstrar-api.sh --banco-off -> inclui o teste de banco indisponível (para e religa o MySQL)
set -uo pipefail
cd "$(dirname "$0")/.."

URL="${SOMBRASP_URL:-http://localhost:8085}"
LOG="docs/evidencias/roteiro_api_mysql.txt"
CONTAINER="${SOMBRASP_MYSQL_CONTAINER:-sombrasp-mysql}"
LOGIN="gestor@sombrasp.dev"
SENHA="${SOMBRASP_SENHA_GESTOR:-Gestor@SombraSP2026}"
mkdir -p docs/evidencias; : > "$LOG"

out() { echo "$*" | tee -a "$LOG"; }
json() { python3 -c 'import sys,json
d=sys.stdin.read()
try: print(json.dumps(json.loads(d), ensure_ascii=False, indent=2))
except Exception: print(d)'; }

# req MÉTODO CAMINHO ESPERADO [args do curl] -> imprime chamada, status e corpo; define CORPO
req() {
  local metodo="$1" caminho="$2" esperado="$3"; shift 3
  local mostrar=("$@"); local i
  for i in "${!mostrar[@]}"; do [[ "${mostrar[$i]}" == Authorization:* ]] && mostrar[$i]="Authorization: Bearer <token do gestor>"; done
  out ""; out "\$ curl -X $metodo $URL$caminho ${mostrar[*]:-}"
  local tmp; tmp="$(mktemp)"
  local status; status="$(curl -s -o "$tmp" -w '%{http_code}' -X "$metodo" "$URL$caminho" "$@")"
  CORPO="$(cat "$tmp")"; rm -f "$tmp"
  out "-> HTTP $status"
  # o token JWT nunca vai para o log
  local exibir; exibir="$(echo "$CORPO" | sed -E 's/("token":")[^"]+/\1<token JWT omitido>/')"
  [[ -n "$exibir" ]] && echo "$exibir" | json | sed 's/^/   /' | tee -a "$LOG" | head -40
  if [[ "$status" == "$esperado" ]]; then out "   ✔ esperado $esperado"; else out "   ✘ esperado $esperado, observado $status"; FALHAS=$((FALHAS+1)); fi
}

mysql_q() {
  out ""; out "[MySQL] $1"
  docker exec "$CONTAINER" sh -c "mysql --default-character-set=utf8mb4 -uroot -p\"\$MYSQL_ROOT_PASSWORD\" sombrasp -t -e \"$1\"" 2>/dev/null | sed 's/^/   /' | tee -a "$LOG"
}

FALHAS=0
HOJE="$(TZ=America/Sao_Paulo date +%F)"
FUTURO="$(TZ=America/Sao_Paulo date -v+5d +%F 2>/dev/null || date -d '+5 day' +%F)"

out "=================================================================="
out "SombraSP | GS 2026.2 SOA e Web Services | Turma 3ESPH"
out "Equipe: Felipe Marques de Oliveira (RM 556319) e Gabriel Barros Cisoto (RM 556309)"
out "API: $URL | Banco: MySQL (container $CONTAINER)"
out "Data local: $(date '+%Y-%m-%d %H:%M:%S %Z') | UTC: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
out "=================================================================="

out ""; out "### 1. Banco e migrações (Flyway)"
mysql_q "SELECT VERSION() AS mysql_versao, DATABASE() AS banco;"
mysql_q "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
mysql_q "SHOW TABLES;"

out ""; out "### 2. Consultas públicas (cidadão / app, sem token)"
req GET /health-check 200
req GET "/pontos?sort=id" 200
req GET "/pontos?bairro=Rep%C3%BAblica" 200
req GET "/pontos/proximos?latitude=-23.5503&longitude=-46.6339&raioKm=1" 200
req GET "/pontos/proximos?latitude=-23.5503&longitude=-46.6339&raioKm=1&tipo=SOMBRA" 200
req GET /alertas 200
req GET /alertas/hoje 200

out ""; out "### 3. Segurança JWT"
req POST /pontos 401 -H 'Content-Type: application/json' -d '{}'
req POST /login 401 -H 'Content-Type: application/json' -d "{\"login\":\"$LOGIN\",\"senha\":\"senha-errada\"}"
req POST /login 200 -H 'Content-Type: application/json' -d "{\"login\":\"$LOGIN\",\"senha\":\"$SENHA\"}"
TOKEN="$(echo "$CORPO" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')"
AUTH="Authorization: Bearer $TOKEN"
out "   (token recebido e usado nas próximas chamadas; não é gravado no log)"

out ""; out "### 4. CRUD de ponto de apoio (gestor)"
req POST /pontos 400 -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"nome":"","tipo":"SOMBRA","latitude":120,"longitude":-46.6,"acessivel":true,"parceiroId":1,"endereco":{"logradouro":"Rua","bairro":"Sé","cidade":"São Paulo","uf":"sp","cep":"123"}}'
req POST /pontos 404 -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"nome":"Ponto sem parceiro","tipo":"SOMBRA","latitude":-23.548,"longitude":-46.6324,"acessivel":true,"parceiroId":999,"endereco":{"logradouro":"Pátio do Colégio","bairro":"Sé","cidade":"São Paulo","uf":"SP","cep":"01016-040"}}'
req POST /pontos 201 -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"nome":"Marquise do Pátio do Colégio","tipo":"SOMBRA","descricao":"Marquise ampla com bancos.","latitude":-23.548,"longitude":-46.6324,"acessivel":true,"horarioFuncionamento":"Livre","parceiroId":1,"endereco":{"logradouro":"Pátio do Colégio","numero":"2","bairro":"Sé","cidade":"São Paulo","uf":"SP","cep":"01016-040"}}'
ID="$(echo "$CORPO" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')"
mysql_q "SELECT id, nome, tipo, bairro, parceiro_id, ativo FROM pontos_apoio WHERE id = $ID;"
req PUT /pontos 200 -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"id\":$ID,\"nome\":\"Marquise do Pátio do Colégio (com água)\",\"tipo\":\"SOMBRA_E_HIDRATACAO\"}"
req DELETE "/pontos/$ID" 204 -H "$AUTH"
req GET "/pontos/$ID" 404
mysql_q "SELECT id, nome, tipo, ativo FROM pontos_apoio WHERE id = $ID;  -- exclusão lógica: a linha continua, com ativo = 0"

out ""; out "### 5. Alertas de calor (regras de negócio)"
req POST /alertas 422 -H "$AUTH" -H 'Content-Type: application/json' -d "{\"dataReferencia\":\"$FUTURO\",\"temperaturaMaxima\":30.5,\"fonte\":\"MANUAL\"}"
req POST /alertas 201 -H "$AUTH" -H 'Content-Type: application/json' -d "{\"dataReferencia\":\"$FUTURO\",\"temperaturaMaxima\":36.0,\"fonte\":\"MANUAL\"}"
AID="$(echo "$CORPO" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')"
req POST /alertas 409 -H "$AUTH" -H 'Content-Type: application/json' -d "{\"dataReferencia\":\"$FUTURO\",\"temperaturaMaxima\":34.0,\"fonte\":\"MANUAL\"}"
req POST /alertas 422 -H "$AUTH" -H 'Content-Type: application/json' -d "{\"dataReferencia\":\"2020-01-01\",\"temperaturaMaxima\":33.0,\"fonte\":\"MANUAL\"}"
req DELETE "/alertas/$AID" 204 -H "$AUTH"
req DELETE "/alertas/$AID" 409 -H "$AUTH"
mysql_q "SELECT id, data_referencia, temperatura_maxima, nivel, fonte, ativo, encerrado_em FROM alertas_calor ORDER BY id;"

if [[ "${1:-}" == "--banco-off" ]]; then
  out ""; out "### 6. Banco indisponível (MySQL parado com a API no ar)"
  out "\$ docker stop $CONTAINER"; docker stop "$CONTAINER" >/dev/null
  req GET "/pontos/1" 503
  out "\$ docker start $CONTAINER"; docker start "$CONTAINER" >/dev/null
  for _ in $(seq 1 30); do docker exec "$CONTAINER" sh -c 'mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null 2>&1 && break; sleep 2; done
  for _ in $(seq 1 15); do [[ "$(curl -s -o /dev/null -w '%{http_code}' "$URL/pontos/1")" == "200" ]] && break; sleep 2; done
  req GET "/pontos/1" 200
fi

out ""
out "Resumo: $(date '+%Y-%m-%d %H:%M:%S %Z') | divergências entre esperado e observado: $FALHAS"
exit $(( FALHAS > 0 ))
