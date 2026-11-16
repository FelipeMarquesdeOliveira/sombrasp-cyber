-- Evolução do esquema: o encerramento do alerta passa a registrar quando aconteceu.
alter table alertas_calor add column encerrado_em datetime;
