create table parceiros(
    id bigint not null auto_increment,
    nome varchar(100) not null,
    tipo varchar(20) not null,
    email varchar(100) not null unique,
    telefone varchar(20),
    ativo boolean not null default true,

    primary key(id)
);
