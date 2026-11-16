create table pontos_apoio(
    id bigint not null auto_increment,
    nome varchar(100) not null,
    tipo varchar(30) not null,
    descricao varchar(300),
    latitude decimal(9,6) not null,
    longitude decimal(9,6) not null,
    acessivel boolean not null,
    horario_funcionamento varchar(60),
    logradouro varchar(100) not null,
    numero varchar(10),
    complemento varchar(50),
    bairro varchar(60) not null,
    cidade varchar(60) not null,
    uf varchar(2) not null,
    cep varchar(9) not null,
    parceiro_id bigint not null,
    ativo boolean not null default true,

    primary key(id),
    constraint fk_pontos_apoio_parceiro_id foreign key(parceiro_id) references parceiros(id)
);
