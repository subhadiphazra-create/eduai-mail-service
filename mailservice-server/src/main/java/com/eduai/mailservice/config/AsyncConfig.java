package com.eduai.mailservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async executor configuration for non-blocking email operations.
 *
 * <p>Separate thread pools for:
 * <ul>
 *   <li>mailExecutor   — general async mail tasks</li>
 *   <li>bulkExecutor   — bulk email processing (lower priority, higher queue)</li>
 * </ul>
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Value("${mail.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${mail.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${mail.async.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("mail-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((r, e) ->
                log.error("mailExecutor rejected task — pool exhausted. Consider increasing pool size."));
        executor.initialize();
        log.info("mailExecutor configured: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }

    @Bean(name = "bulkExecutor")
    public Executor bulkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("mail-bulk-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("bulkExecutor configured: core=2, max=10, queue=2000");
        return executor;
    }
}
