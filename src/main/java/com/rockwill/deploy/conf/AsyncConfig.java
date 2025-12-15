package com.rockwill.deploy.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "rockwillTaskExecutor")
    public ThreadPoolTaskExecutor rockwillTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(cpuCores * 2);
        executor.setMaxPoolSize(cpuCores * 4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("rockwill-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor threadPoolExecutor) -> {
            if (!threadPoolExecutor.isShutdown()) {
                r.run();
            }
        });
        executor.initialize();
        return executor;
    }

}
