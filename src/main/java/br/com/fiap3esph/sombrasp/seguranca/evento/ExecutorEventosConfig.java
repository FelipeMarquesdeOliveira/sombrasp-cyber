package br.com.fiap3esph.sombrasp.seguranca.evento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor dos eventos de segurança: 2 threads em segundo plano (a chamada à IA leva alguns
 * segundos). Nos testes ({@code sombrasp.eventos.sincrono=true}) tudo roda na mesma thread.
 */
@Configuration
public class ExecutorEventosConfig {

    @Bean
    public TaskExecutor executorEventosSeguranca(@Value("${sombrasp.eventos.sincrono:false}") boolean sincrono) {
        if (sincrono) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("risco-cidadao-");
        executor.initialize();
        return executor;
    }
}
