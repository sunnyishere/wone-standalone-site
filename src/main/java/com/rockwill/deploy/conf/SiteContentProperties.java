package com.rockwill.deploy.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 站点内容列表接口配置。
 * 支持默认配置，也支持按域名单独覆盖 base-url / access-key / secret-key，
 * 未覆盖的项回退到默认值。
 */
@Component
@ConfigurationProperties(prefix = "site-content")
@Data
public class SiteContentProperties {
    private String baseUrl = "https://www.iee-business.com";
    private String accessKey;
    private String secretKey;
    private Integer pageSize = 12;
    private Map<String, DomainConfig> domains = new HashMap<>();

    @Data
    /**
     * 单个域名的站点内容接口配置。
     */
    public static class DomainConfig {
        private String accessKey;
        private String secretKey;
    }

    /**
     * 根据域名获取对应的 accessKey，优先使用域名单独配置，未配置时回退到默认值。
     */
    public String getAccessKeyByDomain(String domain) {
        DomainConfig config = domains.get(domain);
        if (config != null && !ObjectUtils.isEmpty(config.getAccessKey())) {
            return config.getAccessKey();
        }
        return accessKey;
    }

    /**
     * 根据域名获取对应的 secretKey，优先使用域名单独配置，未配置时回退到默认值。
     */
    public String getSecretKeyByDomain(String domain) {
        DomainConfig config = domains.get(domain);
        if (config != null && !ObjectUtils.isEmpty(config.getSecretKey())) {
            return config.getSecretKey();
        }
        return secretKey;
    }
}
