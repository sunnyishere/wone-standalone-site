package com.rockwill.deploy.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 站点内容列表接口配置
 */
@Component
@ConfigurationProperties(prefix = "site-content")
@Data
public class SiteContentProperties {
    private String baseUrl = "https://www.iee-business.com";
    private String accessKey;
    private String secretKey;
    private Integer pageSize = 12;
}
