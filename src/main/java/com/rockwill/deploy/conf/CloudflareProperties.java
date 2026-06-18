package com.rockwill.deploy.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "cloudflare")
@Data
/**
 * Cloudflare 缓存清理配置。
 * 支持默认账号配置，也支持按域名单独覆盖 zoneId 和 token。
 */
public class CloudflareProperties {
    private String apiToken;
    private String zoneId;
    private String apiUrl = "https://api.cloudflare.com/client/v4/zones";
    private int purgeBatchSize = 100;
    private long purgeRequestIntervalMs = 1200L;
    private Map<String, DomainConfig> domains = new HashMap<>();

    @Data
    /**
     * 单个域名的 Cloudflare 配置。
     */
    public static class DomainConfig {
        private String zoneId;
        private String apiToken;
    }

    /**
     * 根据域名获取对应的 zoneId，优先使用域名单独配置，未配置时回退到默认值。
     */
    public String getZoneIdByDomain(String domain) {
        DomainConfig config = domains.get(domain);
        if (config != null && !ObjectUtils.isEmpty(config.getZoneId())) {
            return config.getZoneId();
        }
        return zoneId;
    }

    /**
     * 根据域名获取对应的 API Token，优先使用域名单独配置，未配置时回退到默认值。
     */
    public String getApiTokenByDomain(String domain) {
        DomainConfig config = domains.get(domain);
        if (config != null && !ObjectUtils.isEmpty(config.getApiToken())) {
            return config.getApiToken();
        }
        return apiToken;
    }
}
