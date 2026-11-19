# SombraSP · Cybersecurity: Risco Cidadão

Global Solution 2026.2 · FIAP · Engenharia de Software · Turma **3ESPH**

Camada de privacidade e segurança da CitizenTech **SombraSP**, construída sobre a API de SOA do projeto ([sombrasp-soa](https://github.com/FelipeMarquesdeOliveira/sombrasp-soa)). Ela cobre os quatro pontos pedidos:

1. **Parsing de PII:** detecta CPF, RG, dados de saúde e senhas em tudo o que entra e mascara ou criptografa antes de gravar.
2. **Taint Analysis:** rastreia esses dados até as saídas e bloqueia vazamentos em log, console e respostas sem login.
3. **Tradução via IA:** um LLM (MiniMax) transforma o evento técnico numa mensagem simples para o cidadão.
4. **Dashboard:** um painel com farol verde, amarelo e vermelho, mais a mensagem traduzida.

## Entregáveis

| Entregável | Onde está |
|---|---|
| **Source** | Este repositório (também enviado em ZIP no Teams) |
| **Auditoria** | PDF de documentação enviado no Teams; evidências reais em [`docs/evidencias/`](docs/evidencias) |
| **Vídeo do Dashboard** | https://youtu.be/LY8YmtQJia4 (YouTube, não listado, 1 min 36 s) |

## Integrantes

| Nome completo | RM | Turma |
|---|---|---|
| Felipe Marques de Oliveira | 556319 | 3ESPH |
| Gabriel Barros Cisoto | 556309 | 3ESPH |

## Ficha da CitizenTech

| Campo | Valor |
|---|---|
| **Nome** | SombraSP |
| **Cidade e bairro** | São Paulo, Centro (Sé e República) |
| **Cidadão beneficiado** | Idosos e trabalhadores de rua (ambulantes, entregadores, garis) expostos ao sol |
| **Problema, em uma frase** | Nos dias de calor extremo, quem passa o dia na rua não recebe aviso a tempo nem sabe onde encontrar sombra e água por perto. |
| **Como funciona** | Sensores (ESP32) e dados meteorológicos públicos (INMET) estimam o risco de calor. Quando o risco sobe, a plataforma emite alertas e mostra pontos de sombra e hidratação próximos. |
| **Parte de Cybersecurity** | O cidadão se inscreve para receber alertas informando CPF, RG, uma condição de saúde (quem tem hipertensão ou diabetes corre mais risco no calor) e uma senha. Esses dados precisam ser protegidos de ponta a ponta, e o cidadão precisa entender, sem jargão, quando algo foi bloqueado. |
| **Indicador de impacto** | Nº de alertas emitidos em dias de risco e nº de consultas a pontos de sombra e hidratação |
| **ODS conectados** | ODS 3 (Saúde e bem-estar), ODS 10 (Redução das desigualdades), ODS 11 (Cidades sustentáveis), ODS 13 (Ação contra a mudança do clima) |

## Como os quatro pontos foram implementados

```
 ENTRADA (source)                 SAÍDAS VIGIADAS (sinks)                     PAINEL
 ────────────────                 ───────────────────────                     ──────
 JSON / URL ─► FiltroParsingPii ─► RegistroTaint ─┬─► GuardaTaintLog (Logback)  ─┐
               (regex + campo)     (valores        ├─► GuardaTaintConsole (System.out) ├─► EventoSeguranca ─► MiniMax ─► /risco-cidadao ─► /painel
               mascara/cifra       marcados)       └─► GuardaTaintResposta (HTTP) ─┘   (trilha, mascarada)  (LLM)     (farol)
```

| Ponto | O que faz | Arquivos |
|---|---|---|
| **1. Parsing de PII** (3,0) | Middleware que intercepta toda requisição antes do Spring Security. Lê o corpo JSON e os parâmetros da URL, procura CPF (com e sem pontuação, conferindo o dígito verificador), RG, CID-10 e palavras de saúde, senhas pelo nome do campo e senhas escritas em texto livre. Cada achado vira um "tag" de PII. O cadastro grava o CPF mascarado (`***.456.***-00`), um índice HMAC e uma cópia AES-256-GCM; RG e saúde cifrados; senha em BCrypt. | [`DetectorPii`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/pii/DetectorPii.java), [`MascaradorPii`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/pii/MascaradorPii.java), [`CifradorPii`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/pii/CifradorPii.java), [`FiltroParsingPii`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/taint/FiltroParsingPii.java), [`CadastroCidadaoService`](src/main/java/br/com/fiap3esph/sombrasp/service/CadastroCidadaoService.java), [`V7__create-table-cidadaos.sql`](src/main/resources/db/migration/V7__create-table-cidadaos.sql) |
| **2. Taint Analysis** (3,0) | Os valores marcados ficam num registro da requisição. Três guardas consultam esse registro antes de cada saída. **Log:** um `TurboFilter` do Logback descarta a linha (inclusive mensagens de exceção) e grava só um aviso mascarado. **Console:** o `System.out`/`System.err` é trocado por um guarda que não imprime PII (o `console.log(senha)` do Java). **Resposta HTTP:** sem login, a resposta com PII vira um 403 "Resposta bloqueada"; para o gestor autenticado, sai mascarada. Valores decifrados do banco também são marcados. | [`RegistroTaint`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/taint/RegistroTaint.java), [`InspetorSaida`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/taint/InspetorSaida.java), [`GuardaTaintLog`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/taint/GuardaTaintLog.java), [`GuardaTaintConsole`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/taint/GuardaTaintConsole.java), [`GuardaTaintResposta`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/taint/GuardaTaintResposta.java), [`logback-spring.xml`](src/main/resources/logback-spring.xml) |
| **3. Tradução via IA** (2,0) | Cada bloqueio vira um evento técnico, gravado na trilha `eventos_seguranca` só com dados mascarados. Em segundo plano, o backend monta um prompt dinâmico ("Traduza a falha técnica '…' para um cidadão leigo de 60 anos, em 2 frases") e chama o LLM **MiniMax-M3.1-Flash** pela API `chat/completions`. O próprio prompt passa pelo mascarador, então a IA nunca recebe PII. Se a IA falhar ou não houver chave, entra uma mensagem padrão, identificada no painel. | [`PromptRiscoCidadao`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/ia/PromptRiscoCidadao.java), [`TradutorMiniMax`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/ia/TradutorMiniMax.java), [`EventoSegurancaService`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/evento/EventoSegurancaService.java), [`MensagensPadrao`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/ia/MensagensPadrao.java) |
| **4. Dashboard** (2,0) | Página em `/painel/` que atualiza a cada 2 s. Mostra o farol de Risco Cidadão (o nível mais grave da última hora), a mensagem traduzida pela IA com o modelo e o tempo de resposta, indicadores, a trilha de auditoria e um simulador que dispara os cenários de verdade. | [`painel/index.html`](src/main/resources/static/painel/index.html), [`painel.js`](src/main/resources/static/painel/painel.js), [`PainelRiscoService`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/painel/PainelRiscoService.java), [`RiscoCidadaoController`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/painel/RiscoCidadaoController.java) |

### Níveis do farol

| Evento | Nível | Farol |
|---|---|---|
| PII detectada e protegida na entrada; dado mascarado para o gestor | Baixo | 🟢 verde |
| Código tentou gravar PII em log ou no console (bloqueado) | Médio | 🟡 amarelo |
| Resposta sem login tentou levar PII para fora (bloqueada) | Alto | 🔴 vermelho |

### Laboratório de vazamentos

Para provar que as guardas funcionam, [`LaboratorioVazamentoController`](src/main/java/br/com/fiap3esph/sombrasp/seguranca/laboratorio/LaboratorioVazamentoController.java) reproduz **de propósito** três erros comuns de programação, sem nenhuma proteção própria: logar CPF e senha, imprimir o laudo no console e devolver a ficha de saúde decifrada sem exigir login. Quem impede o vazamento são as guardas. Em produção, ele é desligado com `sombrasp.laboratorio.habilitado=false`.

| Rota | Erro simulado | Resultado |
|---|---|---|
| `POST /laboratorio/vazamento-log` | `log.info("cpf={}, senha={}", cpf, senha)` | Linha descartada; aviso mascarado; farol amarelo |
| `POST /laboratorio/vazamento-console` | `System.out.println("Laudo: " + laudo)` | Nada impresso; farol amarelo |
| `GET /laboratorio/ficha-saude/{id}` | Ficha decifrada sem login | HTTP 403 "Resposta bloqueada"; farol vermelho |
| `GET /laboratorio/ficha-saude/{id}` com token de gestor | A mesma ficha | HTTP 200 com tudo mascarado |
| `POST /laboratorio/reiniciar` | Apaga a trilha para recomeçar a demonstração | HTTP 204 |

## Como rodar

Pré-requisitos: **Java 25**, **Docker** (MySQL 8.4) e, para a tradução, uma chave da API da **MiniMax**.

```bash
cp .env.example .env
```

Edite o `.env` (ele está no `.gitignore` e nunca vai para o Git):

- `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` e `JWT_SECRET`: valores fortes;
- `PII_CHAVE`: chave de 32 bytes gerada com `openssl rand -base64 32`;
- `MINIMAX_API_KEY`: chave da MiniMax. Sem ela o sistema funciona, mas usa a mensagem padrão.

```bash
docker compose up -d
```

```bash
./mvnw spring-boot:run
```

O próprio Spring lê o `.env`, configurado em `spring.config.import`. Com a API no ar:

- **Painel:** http://localhost:8086/painel/
- **Swagger:** http://localhost:8086/swagger-ui.html
- **Auditoria automática:** `./scripts/auditar-seguranca.sh`. Roda todos os cenários, consulta o MySQL e confere o log, gravando o resultado em [`docs/evidencias/auditoria_execucao.txt`](docs/evidencias/auditoria_execucao.txt).

Login de gestor de demonstração: `gestor@sombrasp.dev` / `Gestor@SombraSP2026`.

## Testes

```bash
./mvnw test
```

São **82 testes**, todos aprovados ([`docs/evidencias/testes_automatizados.txt`](docs/evidencias/testes_automatizados.txt)): os 50 da API de SOA e 32 da camada de segurança.

- **Detector de PII:** o exemplo do enunciado `{"documento": "123.456.789-00"}` vira `***.456.***-00`; JSON aninhado; texto livre; falsos positivos.
- **Criptografia:** AES-GCM com IV aleatório e detecção de adulteração; índice HMAC.
- **Guardas de log e console:** inclusive CPF dentro de uma exceção.
- **Tradutor MiniMax:** servidor simulado; o teste confere que o prompt não leva o CPF e que há uma mensagem padrão quando a IA falha.
- **Integração ponta a ponta:** com o H2, o teste lê o banco e confirma que nenhuma coluna guarda PII em claro.

## Evidências

| Arquivo | O que mostra |
|---|---|
| [`auditoria_execucao.txt`](docs/evidencias/auditoria_execucao.txt) | Saída real do script de auditoria: requisições e respostas, tabela `cidadaos` no MySQL (só máscara, HMAC, AES-GCM e BCrypt), os 8 eventos com a mensagem da IA e a contagem de valores reais no log (zero) |
| [`console_aplicacao.log`](docs/evidencias/console_aplicacao.log) | Log completo da aplicação durante a auditoria, com as marcas `[PII]`, `[TAINT]` e `[IA]` |
| `painel_01` a `painel_05` (`.png`) | Painel sem riscos, verde, amarelo, vermelho e trilha de auditoria |
| [`testes_automatizados.txt`](docs/evidencias/testes_automatizados.txt) | Resultado do `./mvnw test` |
| [`soa/`](docs/evidencias/soa) | Evidências herdadas da API de SOA (base deste projeto) |

## Decisões e limites

- **Defesa em camadas:**
  - os DTOs com PII reescrevem o `toString()` (um `log.info("{}", dados)` esquecido já sai mascarado);
  - as guardas são a segunda barreira;
  - o banco nunca recebe o valor em claro.
- **O que a IA recebe:** a MiniMax só vê o detalhe técnico mascarado. A chamada é assíncrona, para a requisição do cidadão não esperar o LLM.
- **Limites da detecção:**
  - ela é por regras (regex e nomes de campo), então um formato de documento fora desses padrões pode passar;
  - o registro de contaminação vale para a thread da requisição; trabalho feito em outra thread só é coberto pelos padrões de documento.
- **`spring-boot-devtools` removido:** ele carrega as classes da aplicação num segundo classloader, e o filtro do Logback deixava de enxergar o registro de contaminação. Ferramenta de desenvolvimento também não deve ir para um sistema que trata dados sensíveis.
- **Dados de teste:** todos os dados usados são fictícios. Nenhum dado pessoal real foi usado.

## Uso de IA

O registro do uso de IA no desenvolvimento está em [`prompts.md`](prompts.md).
