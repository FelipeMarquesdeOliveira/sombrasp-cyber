create table alertas_calor(
    id bigint not null auto_increment,
    data_referencia date not null,
    temperatura_maxima decimal(4,1) not null,
    nivel varchar(20) not null,
    fonte varchar(30) not null,
    mensagem varchar(300) not null,
    ativo boolean not null default true,
    emitido_em datetime not null,

    primary key(id)
);
