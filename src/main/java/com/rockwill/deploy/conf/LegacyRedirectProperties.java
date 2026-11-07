package com.rockwill.deploy.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 历史 404 链接兜底 301 配置（legacy-redirect 前缀）。
 * <p>
 * 仅处理 Products / Documents 两类菜单的 404，替代原 nginx cross 逐条规则策略：
 * <ul>
 *   <li>Documents：历史链接缺 {@code detail/} 段导致 404，直接插入 {@code detail/} 段
 *       后 301（同域名同语种）；</li>
 *   <li>Products：产品下架或归属域名迁移导致 404，统一 301 到当前域名的 Products
 *       列表默认链接（保留语种前缀）。</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "legacy-redirect")
@Data
public class LegacyRedirectProperties {

    /**
     * 是否启用历史 404 链接兜底 301；关闭后维持原有 404/302 首页行为
     */
    private Boolean enabled = true;

    /**
     * 取启用开关，未配置时默认启用
     *
     * @return true 表示启用兜底 301
     */
    public boolean resolveEnabled() {
        return enabled == null || enabled;
    }
}
