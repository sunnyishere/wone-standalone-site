package com.rockwill.deploy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 搜索页通用上下文缓存（按 域名+语言 键控）
 * <p>
 * 页面渲染成功时收割白名单内的通用上下文变量（渲染完成态快照），
 * 搜索页渲染时合并使用。仅进程内存持有，无 TTL/持久化；
 * 缓存对象写入后只读共享（容器层浅拷贝，禁止后续加工）。
 */
@Component
@Slf4j
public class SiteCommonContextCache {

    /**
     * 收割白名单：仅这些键允许进入缓存（FR-004）。
     * 口径对齐后端 /menu/topNavPages 链路（SiteApiMenuController.topNavPages →
     * SiteMenuServiceImpl.menu → SiteIndexServiceImpl.index）为页面准备的公共上下文：
     * prodBrand/brandName/siteMenuWidget/topNavPages/bottomNavItems/langList/advanced 之外，
     * 后端还在 Controller 层补充 currentLang/currentPathLang/langName/pageName/websitePath/suffix/fontColor 等键，
     * 搜索页本地渲染时同样需要（head.html 主题色、pc-en 语言切换、导航链接）。
     */
    private static final List<String> WHITELIST = Arrays.asList(
            "prodBrand", "brandName", "siteMenuWidget", "bottomNavItems", "langList", "advanced",
            "currentLang", "currentPathLang", "langName", "pageName", "websitePath", "suffix",
            "fontColor", "headColor", "headFontColor", "otherHeadFontColor", "langEName");

    private final ConcurrentHashMap<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

    /**
     * 收割写入：仅接受白名单键，容器层浅拷贝隔离收割后对源容器的增删
     */
    public void put(String host, String lang, Map<String, Object> model) {
        if (host == null || host.trim().isEmpty() || model == null || model.isEmpty()) {
            return;
        }
        Map<String, Object> snapshot = new HashMap<>();
        for (String key : WHITELIST) {
            Object value = model.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof List) {
                snapshot.put(key, new ArrayList<>((List<?>) value));
            } else if (value instanceof Map) {
                snapshot.put(key, new LinkedHashMap<>((Map<?, ?>) value));
            } else {
                snapshot.put(key, value);
            }
        }
        if (snapshot.isEmpty()) {
            return;
        }
        cache.put(cacheKey(host, lang), snapshot);
    }

    /**
     * 读取（miss 返回 null）
     */
    public Map<String, Object> get(String host, String lang) {
        return host == null ? null : cache.get(cacheKey(host, lang));
    }

    /**
     * 合并进渲染 model（miss 静默跳过）
     */
    public void mergeInto(String host, String lang, Map<String, Object> model) {
        Map<String, Object> snapshot = get(host, lang);
        if (snapshot == null) {
            log.debug("common context cache miss, host:{}, lang:{}", host, lang);
            return;
        }
        model.putAll(snapshot);
    }

    private String cacheKey(String host, String lang) {
        return host + "|" + (lang == null ? "en" : lang);
    }
}
