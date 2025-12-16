package com.rockwill.deploy.vo;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class SecurityReq {
    private String appId;
    private Long timestamp;
    private String nonce;
    private String signature;
    private Object data;

    public boolean hasEssentialParams() {
        return StringUtils.hasText(appId) && timestamp != null &&
                StringUtils.hasText(nonce) && StringUtils.hasText(signature);
    }
}