-- Trilha de auditoria de segurança (PII detectada, vazamentos bloqueados) e mensagem
-- traduzida pela IA para o cidadão. O detalhe técnico é gravado já mascarado.
create table eventos_seguranca(
    id bigint not null auto_increment,
    protocolo char(36) not null unique,
    tipo varchar(40) not null,
    nivel varchar(10) not null,
    saida varchar(10) not null,
    metodo varchar(10),
    caminho varchar(200),
    modulo varchar(60) not null,
    tipos_pii varchar(100),
    detalhe_tecnico varchar(1000) not null,
    mensagem_cidadao varchar(600) not null,
    origem_mensagem varchar(10) not null,
    status_traducao varchar(15) not null,
    modelo_ia varchar(60),
    latencia_ia_ms bigint,
    criado_em datetime not null,
    traduzido_em datetime,

    primary key(id)
);

create index idx_eventos_seguranca_criado_em on eventos_seguranca (criado_em);
