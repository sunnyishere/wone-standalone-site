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
        executor.setCorePoolSize(Math.max(10, cpuCores * 5));
        executor.setMaxPoolSize(Math.max(50, cpuCores * 12));
        executor.setQueueCapacity(2000);
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
