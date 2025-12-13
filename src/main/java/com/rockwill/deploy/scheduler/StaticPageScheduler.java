package com.rockwill.deploy.scheduler;

import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.service.StaticPageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 静态页面生成定时任务调度器
 * 定期自动生成静态页面，避免频繁手动触发
 */
@Component
@Slf4j
public class StaticPageScheduler {

    @Autowired
    private StaticPageService staticPageService;

    @Autowired
    BrandConfig brandConfig;

    /**
     * 每日凌晨2点自动生成所有静态页面
     *
     */
    @Scheduled(cron = "${brand.cron}")
    public void scheduledGenerateAllPages() {
        if (!brandConfig.getSchedulerEnable()) {
            log.error("Static page scheduled tasks are disabled and will be skipped.");
            return;
        }
        String domain = brandConfig.getDomain();
        try {
            log.info("Scheduled task trigger: Start generating all static pages, sites: {}", domain);
            staticPageService.generateAllPages(domain);
        } catch (Exception e) {
            log.error("Scheduled task execution exception,site: {}", domain, e);
        }
        if (brandConfig.getDomainList()!=null && !brandConfig.getDomainList().isEmpty()){
            for (String otherDomain : brandConfig.getDomainList()) {
                staticPageService.generateAllPages(otherDomain);
            }
        }
    }
}