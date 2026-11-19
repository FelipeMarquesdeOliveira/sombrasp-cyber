# Uso de IA

A equipe usou IA durante o desenvolvimento. Este arquivo é o registro combinado na Ficha da CitizenTech.

| Pedido (resumo) | Finalidade | O que foi aproveitado |
|---|---|---|
| Ler o enunciado de Cybersecurity e o PDF de explicação do professor | Entender os 4 pontos (Parsing de PII, Taint Analysis, IA, Dashboard) e os 3 entregáveis | Plano da camada de segurança sobre a API de SOA |
| Projetar o motor de parsing e o Taint Analysis | Decidir as regras de detecção (regex e nome do campo), o registro de contaminação por requisição e as saídas vigiadas (log, console, resposta HTTP) | `seguranca/pii`, `seguranca/taint` |
| Integrar o LLM | Montar o prompt dinâmico, chamar a MiniMax e tratar falhas com mensagem padrão | `seguranca/ia`, `seguranca/evento` |
| Criar o painel de Risco Cidadão | Farol, indicadores, trilha de auditoria e simulador | `src/main/resources/static/painel/` |
| Escrever os testes e o script de auditoria | Provar cada ponto com testes automatizados e evidências reais no MySQL | `src/test/.../seguranca`, `PrivacidadeIntegracaoTest`, `scripts/auditar-seguranca.sh` |
| Gravar o vídeo do dashboard | Roteiro, narração com voz sintetizada (pt-BR, edge-tts) e gravação do painel no Chrome; a pronúncia foi conferida transcrevendo o áudio com o Whisper | Vídeo no YouTube (link no README) |

## IA em tempo de execução

O sistema chama um LLM de propósito (ponto 3 do enunciado):

- **Modelo:** MiniMax-M3.1-Flash, pela API `chat/completions`.
- **O que recebe:** só o detalhe técnico mascarado, nunca PII em claro.
- **Onde fica a chave:** no `.env`, fora do Git.

## Validação prática

- `./mvnw test`: 82 testes aprovados.
- `./scripts/auditar-seguranca.sh` com a API no MySQL:
  - os 8 eventos foram gravados e traduzidos pela IA;
  - zero ocorrências de CPF, RG, senha ou laudo em claro no log da aplicação.
- Ajustes feitos depois de ver o sistema rodando:
  - o `spring-boot-devtools` separava as classes em dois classloaders e o filtro do Logback não enxergava o registro de contaminação; foi removido;
  - a máscara de senha era aplicada duas vezes num texto já mascarado; a regex passou a ignorar valores já mascarados;
  - a MiniMax devolveu uma resposta vazia quando o raciocínio interno gastou todos os tokens; o limite subiu e há uma nova tentativa;
  - o prompt ganhou um exemplo do tom esperado, porque uma resposta sugeria trocar a senha sem necessidade.

## Responsabilidade

O código é da equipe, que responde por ele e precisa saber explicar:

- como o filtro detecta e marca a PII na entrada;
- como cada guarda decide bloquear ou mascarar uma saída;
- por que a IA nunca recebe dado pessoal;
- como o farol é calculado.
