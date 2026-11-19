-- Inscrição do cidadão para alertas. Nenhuma coluna guarda dado pessoal em claro:
-- máscara para exibir, HMAC-SHA256 para achar duplicidade, AES-256-GCM para recuperar
-- e BCrypt para a senha.
create table cidadaos(
    id bigint not null auto_increment,
    apelido varchar(60) not null,
    bairro varchar(60) not null,
    cpf_mascarado varchar(14) not null,
    cpf_indice char(64) not null unique,
    cpf_cifrado varchar(255) not null,
    rg_mascarado varchar(20),
    rg_cifrado varchar(255),
    condicao_saude_cifrada varchar(1024),
    senha_hash varchar(100) not null,
    aceita_alertas boolean not null default true,
    criado_em datetime not null,

    primary key(id)
);
