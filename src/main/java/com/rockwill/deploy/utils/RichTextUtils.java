package com.rockwill.deploy.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CMS 富文本骨架清理工具
 * <p>
 * CMS 编辑器粘贴网页源码时，会把整份 HTML 文档（{@code <!DOCTYPE html><html><head>...</head>
 * <body>...</body></html>}）存进富文本字段（如 prod.detail、news.content、article.content、
 * blog.content、successCase.content）。该骨架经 {@code th:utext} 原样输出后，页面会出现
 * 第二套 head/body（Screaming Frog「Validation: Multiple head/body Tags」的根因）。
 * 本工具负责剥离文档骨架，仅保留正文内容。
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

    private RichTextUtils() {
    }

    /**
     * 剥离富文本中被整体粘贴的 HTML 文档骨架，仅保留实际内容。
     * <p>
     * 处理顺序：先逐块把 body 区块替换为其内部内容（支持同一字段内多份文档骨架），
     * 再移除残留的 head 区块、DOCTYPE 声明与 html 开闭标签。
     * 无骨架时原样返回，不做任何改写。
     *
     * @param html CMS 下发的富文本字符串
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
}
