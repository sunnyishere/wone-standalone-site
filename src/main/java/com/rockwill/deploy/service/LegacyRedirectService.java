package com.rockwill.deploy.service;

import com.rockwill.deploy.conf.LegacyRedirectProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 历史 404 链接兜底 301 服务（仅处理 Products / Documents 两类菜单）。
 * <p>
 * 替代原 nginx cross 逐条规则策略（每条 404 单独配跨域名 301，维护成本高）：
 * <ul>
 *   <li>Documents：历史链接缺 {@code detail/} 段导致 404（CMS 详情接口按尾部数字 ID
 *       解析、不校验 slug），直接在原始路径插入 {@code detail/} 段后 301，同域名同语种；</li>
 *   <li>Products：产品下架或归属域名迁移导致 404，统一 301 到当前域名的 Products
 *       列表默认链接（保留语种前缀），不探测 CMS。</li>
 * </ul>
 * nginx 既有 301 规则在前，本服务只接漏网请求。
 */
@Service
@Slf4j
public class LegacyRedirectService {

    private static final Pattern LANG_PREFIX = Pattern.compile("^/([a-z]{2}(?:-[A-Za-z]{2})?)(?=/)");

    private static final String MENU_DOCUMENTS = "Documents";
    private static final String MENU_PRODUCTS = "Products";

    @Autowired
    private LegacyRedirectProperties properties;

    /**
     * 解析历史 404 请求的 301 目标。
     *
     * @param host 归一后的请求域名（PathMatchingInterceptor resolvedHost 口径）
     * @param uri  原始请求 URI（含语种前缀与查询串，如 /pt/Documents/default-291?x=1）
     * @return 301 目标绝对地址；无法推断时返回 null（调用方维持原 404 行为）
     */
    public String resolveRedirect(String host, String uri) {
        if (!properties.resolveEnabled() || host == null || uri == null) {
            return null;
        }
        ParsedRequest parsed = parse(uri);
        if (parsed == null) {
            return null;
        }
        if (MENU_DOCUMENTS.equalsIgnoreCase(parsed.menuName)) {
            return buildDocumentsDetail(host, parsed);
        }
        if (MENU_PRODUCTS.equalsIgnoreCase(parsed.menuName)) {
            return buildProductsList(host, parsed);
        }
        return null;
    }

    /**
     * Documents 兜底：原始路径插入 {@code detail/} 段，同域名同语种 301。
     * CMS 详情接口按尾部数字 ID 解析，插入后单跳直达 canonical；
     * 插入后仍 404 时本服务对 detail/ 形态不再兜底，无重定向循环。
     *
     * @param host   当前域名
     * @param parsed 解析后的请求要素
     * @return 301 目标绝对地址
     */
    private String buildDocumentsDetail(String host, ParsedRequest parsed) {
        StringBuilder sb = new StringBuilder(schemePrefix(host));
        if (parsed.lang != null && !parsed.lang.isEmpty()) {
            sb.append('/').append(parsed.lang);
        }
        sb.append('/').append(MENU_DOCUMENTS).append("/detail/").append(parsed.lastSegment);
        return sb.toString();
    }

    /**
     * Products 兜底：统一 301 到当前域名 Products 列表默认链接（保留语种前缀）。
     *
     * @param host   当前域名
     * @param parsed 解析后的请求要素
     * @return 301 目标绝对地址
     */
    private String buildProductsList(String host, ParsedRequest parsed) {
        StringBuilder sb = new StringBuilder(schemePrefix(host));
        if (parsed.lang != null && !parsed.lang.isEmpty()) {
            sb.append('/').append(parsed.lang);
        }
        sb.append('/').append(MENU_PRODUCTS);
        return sb.toString();
    }

    /**
     * 解析历史 404 URI：剥离语种前缀，按菜单分段。
     * Documents 仅处理 /{lang}/Documents/{slug} 单段缺 detail/ 形态；
     * Products 处理任意 /{lang}/Products/** 多段形态；菜单页本身（/Products、/Documents）
     * 与其它菜单不兜底。
     *
     * @param uri 原始请求 URI
     * @return 解析结果；不属于兜底形态时返回 null
     */
    private ParsedRequest parse(String uri) {
        String path = uri;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        String lang = null;
        Matcher lm = LANG_PREFIX.matcher(path);
        if (lm.find()) {
            lang = lm.group(1);
            path = path.substring(lm.end());
        }
        if (path.endsWith("/") || path.contains("//")) {
            return null;
        }
        String[] segments = path.substring(1).split("/");
        // 菜单页本身（/Products、/Documents）由 CMS 直接渲染，不兜底
        if (segments.length < 2) {
            return null;
        }
        String menuName = segments[0];
        boolean documents = MENU_DOCUMENTS.equalsIgnoreCase(menuName);
        boolean products = MENU_PRODUCTS.equalsIgnoreCase(menuName);
        if (!documents && !products) {
            return null;
        }
        if (documents && (segments.length != 2 || "detail".equalsIgnoreCase(segments[1]))) {
            // detail/ 形态由 CMS 自身处理（真 404 即内容不存在）；
            // 多级路径插入 detail/ 后无法被 DETAIL 规则识别，均不兜底
            return null;
        }
        ParsedRequest parsed = new ParsedRequest();
        parsed.lang = lang;
        parsed.menuName = menuName;
        parsed.lastSegment = segments[segments.length - 1];
        return parsed;
    }

    private String schemePrefix(String host) {
        return "http://" + host;
    }

    /**
     * 解析后的请求要素
     */
    private static class ParsedRequest {
        String lang;
        String menuName;
        String lastSegment;
    }
}
