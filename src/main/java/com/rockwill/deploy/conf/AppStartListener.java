package com.rockwill.deploy.conf;

import com.rockwill.deploy.service.StaticPageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class AppStartListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    StaticPageService staticPageService;
    @Autowired
    BrandConfig brandConfig;
    @Autowired
    @Qualifier("rockwillTaskExecutor")
    private Executor taskExecutor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (brandConfig.getExecuteOnStart()) {
            taskExecutor.execute(() -> {
                log.info("Start asynchronous execution of static page generation tasks");
                staticPageService.generateAllPages();
            });
        }
    }
}
