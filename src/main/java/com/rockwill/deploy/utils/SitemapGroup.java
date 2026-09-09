package com.rockwill.deploy.utils;

import com.redfin.sitemapgenerator.ChangeFreq;
import lombok.Data;
import lombok.Getter;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聚合同一页面在不同语言下的 URL 信息，用于生成 xhtml:link 关系。
 */
@Data
public class SitemapGroup {

    /** 不含语言前缀的 URI，如 "products/detail/abc-123" */
    private String uriBase;

    /** 对应 SitePage.SitePageType 常量（1-8） */
    private int pageType;

    /** 语言 → 条目 */
    private Map<String, LangEntry> langEntries = new ConcurrentHashMap<>();

    /** 最后修改时间 */
    private Date lastMod = new Date();

    /** 变更频率 */
    private ChangeFreq changeFreq = ChangeFreq.DAILY;

    public SitemapGroup() {
    }

    public SitemapGroup(String uriBase, int pageType) {
        this.uriBase = uriBase;
        this.pageType = pageType;
    }

    /**
     * 单一语言变体的信息。
     */
    public static class LangEntry {

        /** 带语言前缀的完整 URI，如 "en/products/detail/abc-123" */
        private String uri;

        /** 语言代码（如 "en"、"zh"），用于 xhtml:link 的 hreflang 属性 */
        private String langCode;

        /** 页面优先级（0.0-1.0） */
        private double priority;

        /** 是否为默认语言（用作 x-default） */
        private boolean isDefault;

        public LangEntry() {
        }

        public LangEntry(String uri, String langCode, double priority, boolean isDefault) {
            this.uri = uri;
            this.langCode = langCode;
            this.priority = priority;
            this.isDefault = isDefault;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getLangCode() {
            return langCode;
        }

        public void setLangCode(String langCode) {
            this.langCode = langCode;
        }

        public double getPriority() {
            return priority;
        }

        public void setPriority(double priority) {
            this.priority = priority;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean isDefault) {
            this.isDefault = isDefault;
        }
    }
}
