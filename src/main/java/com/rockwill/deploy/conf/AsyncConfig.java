package com.rockwill.deploy.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Resource
    BrandConfig brandConfig;

    @Bean(name = "rockwillTaskExecutor")
    public ThreadPoolTaskExecutor rockwillTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(brandConfig.resolveStaticPoolCore());
        executor.setMaxPoolSize(brandConfig.resolveStaticPoolMax());
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
