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
    private Boolean schedulerEnable;
    private Boolean executeOnStart;
    private List<String> executeOnDomain;
    private List<String> languages;
    private List<String> domainList;
    private String secret;
}
