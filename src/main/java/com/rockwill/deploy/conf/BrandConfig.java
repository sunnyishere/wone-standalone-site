package com.rockwill.deploy.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "brand")
@Data
public class BrandConfig {
    private String domain;
    private String staticOutput;
    private String requestLogDir;
    private Boolean schedulerEnable;
    private Boolean executeOnStart;
    private List<String> executeOnDomain;
    private List<String> languages;
    private List<String> mapLang;
    private List<String> domainList;
    private String secret;
    private Boolean caseTranslateOnline;
    /**
     * 发往知识库（wcm-api）请求的全局最大并发数，未配置时默认 5
     */
    private Integer wcmApiConcurrency;
    /**
     * 静态化线程池核心线程数，未配置时默认 5
     */
    private Integer staticPoolCore;
    /**
     * 静态化线程池最大线程数，未配置时默认 8
     */
    private Integer staticPoolMax;

    public int resolveStaticPoolCore() {
        return staticPoolCore == null ? 5 : Math.max(1, staticPoolCore);
    }

    /**
     * 池上限 MUST 不小于 wcm-api 并发许可数，否则 Semaphore 限流会被池大小截断；
     * 同时不小于核心线程数，保证线程池参数自洽
     */
    public int resolveStaticPoolMax() {
        int min = Math.max(resolveStaticPoolCore(), resolveWcmApiConcurrency());
        return staticPoolMax == null ? Math.max(8, min) : Math.max(min, staticPoolMax);
    }

    public int resolveWcmApiConcurrency() {
        return wcmApiConcurrency == null ? 5 : Math.max(1, wcmApiConcurrency);
    }
}
