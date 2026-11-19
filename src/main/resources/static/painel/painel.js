// Painel Risco Cidadão: lê GET /risco-cidadao a cada 2 s e dispara os cenários do laboratório.
(() => {
    const $ = (id) => document.getElementById(id);
    const vistos = new Set();
    let primeiraCarga = true;
    let cidadaoId = null;

    const NOMES_TIPO = {
        PII_DETECTADA: 'Dado pessoal protegido na entrada',
        DADO_MASCARADO_NA_RESPOSTA: 'Dado mascarado para gestor',
        VAZAMENTO_LOG_BLOQUEADO: 'Vazamento em log bloqueado',
        VAZAMENTO_CONSOLE_BLOQUEADO: 'Vazamento no console bloqueado',
        VAZAMENTO_RESPOSTA_BLOQUEADO: 'Resposta sem login bloqueada',
    };
    const TITULO_NIVEL = { VERDE: 'Baixo', AMARELO: 'Atenção', VERMELHO: 'Alto' };

    function escapar(texto) {
        return String(texto ?? '').replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        }[c]));
    }

    function hora(iso) {
        return iso ? iso.substring(11, 19) : '--:--:--';
    }

    // CPF fictício gerado na hora (dígitos verificadores válidos), para não repetir cadastro.
    function cpfFicticio() {
        const d = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
        for (const tamanho of [9, 10]) {
            const soma = d.slice(0, tamanho).reduce((s, n, i) => s + n * (tamanho + 1 - i), 0);
            const resto = soma % 11;
            d.push(resto < 2 ? 0 : 11 - resto);
        }
        const s = d.join('');
        return `${s.slice(0, 3)}.${s.slice(3, 6)}.${s.slice(6, 9)}-${s.slice(9)}`;
    }

    function rgFicticio() {
        const n = String(Math.floor(10000000 + Math.random() * 89999999));
        return `${n.slice(0, 2)}.${n.slice(2, 5)}.${n.slice(5, 8)}-${Math.floor(Math.random() * 10)}`;
    }

    let dados = null;
    function novosDados() {
        dados = {
            apelido: 'Dona Maria (fictícia)',
            bairro: 'Sé',
            cpf: cpfFicticio(),
            rg: rgFicticio(),
            condicaoSaude: 'Hipertensão (CID I10), usa diurético',
            senha: 'Calor@' + Math.floor(1000 + Math.random() * 9000),
            aceitaAlertas: true,
        };
        return dados;
    }

    function selo(painel, eventos) {
        const principal = eventos.find((e) => e.protocolo === painel.protocoloPrincipal);
        if (painel.statusTraducao === 'PENDENTE') {
            return '<span class="selo pendente"><span class="giro"></span>Traduzindo com IA…</span>'
                + '<span>Mensagem padrão até a IA responder</span>';
        }
        if (painel.origemMensagem === 'IA') {
            const s = principal && principal.latenciaIaMs ? (principal.latenciaIaMs / 1000).toFixed(1).replace('.', ',') + ' s' : '';
            return `<span class="selo ia">Traduzido por IA · ${escapar(painel.modeloIa)}</span><span>${s}</span>`;
        }
        const motivo = painel.statusTraducao === 'FALHOU' ? ' (IA indisponível)'
            : painel.statusTraducao === 'DESATIVADA' ? ' (IA desligada)' : '';
        return `<span class="selo padrao">Mensagem padrão${motivo}</span>`;
    }

    function destacarEtapa(painel, eventos) {
        ['etapa-entrada', 'etapa-taint', 'etapa-ia', 'etapa-painel'].forEach((id) => $(id).classList.remove('ativo'));
        if (!eventos.length) return;
        const ultimo = eventos[0];
        if (ultimo.statusTraducao === 'PENDENTE') {
            $(ultimo.saida === 'ENTRADA' ? 'etapa-entrada' : 'etapa-taint').classList.add('ativo');
            $('etapa-ia').classList.add('ativo');
        } else {
            $('etapa-painel').classList.add('ativo');
        }
    }

    function render(painel) {
        $('farol').className = 'farol ' + painel.farol;
        $('farol').setAttribute('aria-label', 'Farol de risco: ' + (TITULO_NIVEL[painel.farol] || painel.farol));
        document.querySelector('.principal').className = 'cartao principal ' + painel.farol;
        $('tituloRisco').textContent = painel.titulo;
        $('mensagem').textContent = painel.mensagemCidadao;
        $('origem').innerHTML = selo(painel, painel.eventos)
            + (painel.protocoloPrincipal ? `<span>· protocolo ${escapar(painel.protocoloPrincipal.substring(0, 8))}</span>` : '');
        $('atualizadoEm').textContent = hora(painel.atualizadoEm);

        const ind = painel.indicadores;
        $('indProtegidos').textContent = ind.dadosProtegidos;
        $('indLog').textContent = ind.vazamentosBloqueadosLog;
        $('indResposta').textContent = ind.respostasBloqueadas;
        $('indIa').textContent = ind.traducoesIa;
        $('totalEventos').textContent = `${ind.totalEventos} evento(s) na última hora`;

        const corpo = $('eventos');
        if (!painel.eventos.length) {
            corpo.innerHTML = '<tr class="vazio"><td colspan="6">Nenhum evento ainda.</td></tr>';
        } else {
            corpo.innerHTML = painel.eventos.map((e) => {
                const novo = !primeiraCarga && !vistos.has(e.protocolo);
                vistos.add(e.protocolo);
                const origem = e.statusTraducao === 'PENDENTE' ? 'traduzindo com IA…'
                    : e.origemMensagem === 'IA' ? `IA · ${escapar(e.modeloIa)}` : 'mensagem padrão';
                return `<tr class="${novo ? 'novo' : ''}">
                    <td class="hora">${hora(e.criadoEm)}</td>
                    <td><span class="chip ${e.cor}">${TITULO_NIVEL[e.cor] || e.cor}</span></td>
                    <td>${escapar(NOMES_TIPO[e.tipo] || e.descricao)}<span class="mini">${escapar(e.saida)} · ${escapar(e.metodo || '')} ${escapar(e.caminho || '')}</span></td>
                    <td>${escapar(e.modulo)}</td>
                    <td class="detalhe">${escapar(e.detalheTecnico)}</td>
                    <td class="msg">${escapar(e.mensagemCidadao)}<span class="mini">${origem}</span></td>
                </tr>`;
            }).join('');
        }
        destacarEtapa(painel, painel.eventos);
        primeiraCarga = false;
    }

    async function atualizar() {
        try {
            const resposta = await fetch('/risco-cidadao', { cache: 'no-store' });
            if (resposta.ok) render(await resposta.json());
        } catch (e) {
            $('atualizadoEm').textContent = 'sem conexão';
        }
    }

    async function chamar(metodo, caminho, corpo) {
        const resposta = await fetch(caminho, {
            method: metodo,
            headers: corpo ? { 'Content-Type': 'application/json' } : {},
            body: corpo ? JSON.stringify(corpo) : undefined,
        });
        const texto = await resposta.text();
        let json = null;
        try { json = texto ? JSON.parse(texto) : null; } catch (e) { /* corpo vazio */ }
        return { status: resposta.status, json };
    }

    function mostrar(linhas) {
        $('resultado').textContent = linhas.join('\n');
    }

    async function cadastrar() {
        const d = novosDados();
        const r = await chamar('POST', '/cidadaos', d);
        if (r.json && r.json.id) cidadaoId = r.json.id;
        mostrar([
            `Enviado (fictício): CPF ${d.cpf} · RG ${d.rg} · saúde: ${d.condicaoSaude} · senha: ${'•'.repeat(d.senha.length)}`,
            `POST /cidadaos  →  HTTP ${r.status}`,
            r.json && r.json.cpf
                ? `Gravado e devolvido só mascarado: CPF ${r.json.cpf} · RG ${r.json.rg} · saúde ${r.json.condicaoSaude}`
                : JSON.stringify(r.json),
        ]);
    }

    const acoes = {
        cadastro: cadastrar,
        async log() {
            const d = dados || novosDados();
            const r = await chamar('POST', '/laboratorio/vazamento-log', { cpf: d.cpf, senha: d.senha });
            mostrar([
                'Código com erro: log.info("Depuração do cadastro: cpf={}, senha={}", cpf, senha)',
                `POST /laboratorio/vazamento-log  →  HTTP ${r.status}`,
                'A linha com CPF e senha foi descartada pelo GuardaTaintLog. No log ficou só o aviso mascarado.',
            ]);
        },
        async console() {
            const d = dados || novosDados();
            const r = await chamar('POST', '/laboratorio/vazamento-console', { condicaoSaude: d.condicaoSaude });
            mostrar([
                'Código com erro: System.out.println("Laudo recebido do cidadão: " + condicaoSaude)',
                `POST /laboratorio/vazamento-console  →  HTTP ${r.status}`,
                'O GuardaTaintConsole não deixou o laudo ser impresso.',
            ]);
        },
        async resposta() {
            if (!cidadaoId) await cadastrar();
            const r = await chamar('GET', `/laboratorio/ficha-saude/${cidadaoId}`);
            mostrar([
                `GET /laboratorio/ficha-saude/${cidadaoId} (sem login)  →  HTTP ${r.status}`,
                r.json ? `${r.json.erro || ''}: ${r.json.mensagem || JSON.stringify(r.json)}` : '',
                r.json && r.json.protocolo ? `Protocolo do bloqueio: ${r.json.protocolo}` : '',
            ]);
        },
    };

    document.querySelectorAll('.acao').forEach((botao) => {
        botao.addEventListener('click', async () => {
            botao.classList.add('rodando');
            try {
                await acoes[botao.dataset.acao]();
            } catch (e) {
                mostrar(['Falha ao chamar a API: ' + e.message]);
            } finally {
                botao.classList.remove('rodando');
                atualizar();
            }
        });
    });

    $('btnReiniciar').addEventListener('click', async () => {
        await chamar('POST', '/laboratorio/reiniciar');
        vistos.clear();
        primeiraCarga = true;
        mostrar(['Eventos apagados. O farol volta para verde.']);
        atualizar();
    });

    atualizar();
    setInterval(atualizar, 2000);
})();
