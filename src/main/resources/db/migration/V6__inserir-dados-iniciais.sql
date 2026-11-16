-- Carga inicial (dados fictícios ou públicos; coordenadas aproximadas do Centro de SP).
-- Nenhum dado pessoal real.

insert into parceiros (nome, tipo, email, telefone, ativo) values
('Subprefeitura Sé (parceria proposta)', 'PODER_PUBLICO', 'contato.se@parceiro-ficticio.sombrasp.dev', null, true),
('Associação de Ambulantes do Centro (fictícia)', 'ONG', 'ambulantes@parceiro-ficticio.sombrasp.dev', null, true),
('Comércio Parceiro Rua Direita (fictício)', 'COMERCIO', 'loja@parceiro-ficticio.sombrasp.dev', null, true);

insert into pontos_apoio (nome, tipo, descricao, latitude, longitude, acessivel, horario_funcionamento,
    logradouro, numero, complemento, bairro, cidade, uf, cep, parceiro_id, ativo) values
('Tenda de hidratação da Praça da Sé', 'SOMBRA_E_HIDRATACAO', 'Tenda com sombra e kits de água nos dias de alerta.',
    -23.550300, -46.633900, true, '08h às 18h', 'Praça da Sé', 's/n', 'Saída do Metrô Sé', 'Sé', 'São Paulo', 'SP', '01001-000', 1, true),
('Sombra das árvores da Praça da República', 'SOMBRA', 'Área arborizada com bancos.',
    -23.543500, -46.642600, true, 'Livre', 'Praça da República', 's/n', null, 'República', 'São Paulo', 'SP', '01045-000', 1, true),
('Ponto de água dos ambulantes', 'HIDRATACAO', 'Bebedouro mantido pela associação.',
    -23.545700, -46.632100, false, '07h às 19h', 'Rua 25 de Março', '1000', null, 'Sé', 'São Paulo', 'SP', '01021-000', 2, true),
('Loja parceira com água gratuita', 'HIDRATACAO', 'Comércio que oferece água gratuita.',
    -23.548600, -46.635500, true, '09h às 18h', 'Rua Direita', '100', null, 'Sé', 'São Paulo', 'SP', '01002-000', 3, true);

insert into alertas_calor (data_referencia, temperatura_maxima, nivel, fonte, mensagem, ativo, emitido_em) values
('2026-11-18', 34.2, 'ALERTA', 'ESTACAO_ESP32', 'Calor forte agora no Centro. Procure sombra e beba água a cada 20 minutos.', true, '2026-11-18 10:00:00'),
('2026-11-19', 33.5, 'ALERTA', 'MODELO_IA_INMET', 'Amanhã a máxima deve passar de 31 °C. Planeje pausas e leve água.', true, '2026-11-18 18:00:00');

-- Usuário gestor de demonstração (senha documentada no README, guardada como hash BCrypt).
insert into usuarios (nome, login, senha, perfil) values
('Gestor SombraSP (demonstração)', 'gestor@sombrasp.dev', '$2a$10$LjDFXgVGclr1tPTLcddiHuxslFKMnuSez3HFAZA81Zmm7ZzOQssC6', 'GESTOR');
