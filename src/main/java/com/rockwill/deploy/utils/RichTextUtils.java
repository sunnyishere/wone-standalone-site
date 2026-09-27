package com.rockwill.deploy.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 富文本清理工具
 * <p>
 * 处理两类富文本下发内容的质量问题：
 * 1. HTML 文档（{@code <!DOCTYPE html><html><head>...</head>
 * <body>...</body></html>}）存进富文本字段（如 prod.detail、news.content、article.content、
 * blog.content、successCase.content）；
 * 2. 富文本内 UEditor 上传的图片缺 alt 属性
 */
public final class RichTextUtils {

    /**
     * 匹配文档骨架中的 body 区块（非贪婪，支持同一字段内多份被粘贴的文档）
     */
    private static final Pattern BODY_PATTERN = Pattern.compile("(?is)<body[^>]*>(.*?)</body>");

    /**
     * 匹配文档骨架中的 head 区块
     */
    private static final Pattern HEAD_PATTERN = Pattern.compile("(?is)<head[^>]*>.*?</head>");

    /**
     * 匹配 html 开闭标签（含属性）
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?i)</?html[^>]*>");

    /**
     * 匹配 DOCTYPE 声明
     */
    private static final Pattern DOCTYPE_PATTERN = Pattern.compile("(?i)<!DOCTYPE[^>]*>");

    /**
     * 匹配 img 标签（DOTALL，容忍属性值跨行）
     */
    private static final Pattern IMG_PATTERN = Pattern.compile("(?i)<img\\b[^>]*>");

    /**
     * 匹配 img 标签上的 alt 属性
     */
    private static final Pattern ALT_ATTR_PATTERN = Pattern.compile("(?i)\\balt\\s*=");

    private RichTextUtils() {
    }

    /**
     * 剥离富文本中被整体粘贴的 HTML 文档骨架，仅保留实际内容。
     * <p>
     * 处理顺序：先逐块把 body 区块替换为其内部内容（支持同一字段内多份文档骨架），
     * 再移除残留的 head 区块、DOCTYPE 声明与 html 开闭标签。
     * 无骨架时原样返回，不做任何改写。
     *
     * @param html 富文本字符串
     * @return 剥离文档骨架后的内容；入参为空或不含骨架时原样返回
     */
    public static String stripDocumentSkeleton(String html) {
        if (StringUtils.isBlank(html)) {
            return html;
        }
        String cleaned = html;
        if (StringUtils.containsIgnoreCase(cleaned, "<body")) {
            Matcher bodyMatcher = BODY_PATTERN.matcher(cleaned);
            StringBuffer replaced = new StringBuffer();
            boolean found = false;
            while (bodyMatcher.find()) {
                found = true;
                bodyMatcher.appendReplacement(replaced, Matcher.quoteReplacement(bodyMatcher.group(1)));
            }
            if (found) {
                bodyMatcher.appendTail(replaced);
                cleaned = replaced.toString();
            }
        }
        if (StringUtils.containsIgnoreCase(cleaned, "<!doctype")
                || StringUtils.containsIgnoreCase(cleaned, "<html")
                || StringUtils.containsIgnoreCase(cleaned, "<head")) {
            cleaned = HEAD_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = DOCTYPE_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
            return cleaned.trim();
        }
        if (!cleaned.equals(html)) {
            return cleaned.trim();
        }
        return html;
    }

    /**
     * 为富文本中缺失 alt 属性的 img 标签补齐 alt，兜底值用所在实体名称。
     * <p>
     * 仅补「完全缺失 alt 属性」的标签；已有 alt（含显式 {@code alt=""}）保持原样，
     * 不覆盖编辑侧的语义标注。同一字段内多张图共用同一兜底值。
     *
     * @param html   富文本字符串
     * @param altText 兜底 alt 文本（如产品/文章名称），为空时不做任何改写
     * @return 补齐 alt 后的内容；入参为空、无 img 或兜底值为空时原样返回
     */
    public static String fillMissingAlt(String html, String altText) {
        if (StringUtils.isBlank(html) || StringUtils.isBlank(altText)) {
            return html;
        }
        if (!StringUtils.containsIgnoreCase(html, "<img")) {
            return html;
        }
        String escapedAlt = escapeAttrValue(altText.trim());
        Matcher imgMatcher = IMG_PATTERN.matcher(html);
        StringBuffer replaced = new StringBuffer();
        boolean changed = false;
        while (imgMatcher.find()) {
            String tag = imgMatcher.group(0);
            if (!ALT_ATTR_PATTERN.matcher(tag).find()) {
                tag = insertAltAttr(tag, escapedAlt);
                changed = true;
            }
            imgMatcher.appendReplacement(replaced, Matcher.quoteReplacement(tag));
        }
        imgMatcher.appendTail(replaced);
        return changed ? replaced.toString() : html;
    }

    /**
     * 在 img 标签闭合前插入 alt 属性，保留原有的自闭合斜杠风格。
     *
     * @param tag       原始 img 标签
     * @param escapedAlt 已转义的 alt 文本
     * @return 插入 alt 后的标签
     */
    private static String insertAltAttr(String tag, String escapedAlt) {
        String trimmed = tag.trim();
        if (trimmed.endsWith("/>")) {
            return trimmed.substring(0, trimmed.length() - 2).trim() + " alt=\"" + escapedAlt + "\"/>";
        }
        return trimmed.substring(0, trimmed.length() - 1).trim() + " alt=\"" + escapedAlt + "\">";
    }

    /**
     * 转义 alt 属性值中的引号与 &amp;，防止破坏标签结构。
     *
     * @param value 原始文本
     * @return 属性安全文本
     */
    private static String escapeAttrValue(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;");
    }

    /**
     * 将内容中白名单 OSS host 的 http 资源地址升级为 https，消除混合内容。
     *
     *
     * @param html 含资源地址的内容字符串
     * @return 升级 https 后的内容；不含白名单前缀时原样返回
     */
    public static String upgradeOssToHttps(String html) {
        if (StringUtils.isBlank(html) || !html.contains("http://oss.")) {
            return html;
        }
        String cleaned = html;
        cleaned = cleaned.replaceAll("http://oss.iwone.cn", "https://oss.iee-business.com")
                .replaceAll("http://oss.iee-business.com", "https://oss.iee-business.com");
        return cleaned;
    }
}
