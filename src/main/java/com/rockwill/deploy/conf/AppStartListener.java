package com.rockwill.deploy.conf;

import com.rockwill.deploy.service.RockwillKnowledgeService;
import com.rockwill.deploy.service.StaticPageService;
import com.rockwill.deploy.utils.PathMatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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
    @Autowired
    RockwillKnowledgeService knowledgeService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            staticPageService.copyStaticResources("");
        } catch (IOException e) {
            log.warn("copy static error:{}",e.getMessage());
        }
        knowledgeService.getSiteMenu(brandConfig.getDomain());
        if (brandConfig.getDomainList()!=null && !brandConfig.getDomainList().isEmpty()){
            for (String domain : brandConfig.getDomainList()) {
                knowledgeService.getSiteMenu(domain);
            }
        }
        if (brandConfig.getExecuteOnStart()) {
            if (brandConfig.getExecuteOnDomain() != null && !brandConfig.getExecuteOnDomain().isEmpty()) {
                for (String domain : brandConfig.getExecuteOnDomain()) {
                    staticPageService.generateAllPages(domain);
                }
            }
        }
    }
}
