package com.rockwill.deploy.utils;

import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文章类详情页 about 结构化数据构建工具
 * <p>
 * 从 SitePageType 3/5/7/8（Solutions/News/Success Reference/Blog）详情页富文本正文中
 * 提取站内 a 标签链接及 title，结合链接目标所属菜单（SitePage）的 pageName/pageType
 * 做实体区分，生成 schema.org {@code about} 数组，合并进页面 model 既有
 * {@code schemaJson}（CMS 下发的 Map 形态，由 handleSchemaJson 统一序列化）：
 * <ul>
 *   <li>链接指向 Products 菜单 → {@code Product}</li>
 *   <li>链接指向 Solutions 菜单 → {@code TechArticle}</li>
 *   <li>链接指向 News 菜单 → {@code NewsArticle}</li>
 *   <li>链接指向 Success Reference 菜单 → {@code TechArticle}</li>
 *   <li>链接指向 Blog 菜单 → {@code BlogPosting}</li>
 * </ul>
 * 即与链接目标页面自身声明的类型口径一致：2=Product、3=TechArticle、5=NewsArticle、
 * 7=TechArticle、8=BlogPosting。
 */
@Slf4j
public final class ArticleAboutUtils {

    /**
     * 文章类详情页实体 key → 富文本内容字段名（与 RockwillKnowledgeService.RICH_TEXT_FIELDS 同口径）
     */
    private static final Map<String, String> RICH_TEXT_FIELDS = new LinkedHashMap<String, String>() {{
        put("article", "content");
        put("news", "content");
        put("successCase", "content");
        put("blog", "content");
    }};

    /**
     * SitePageType → about 条目 @type 映射
     */
    private static final Map<Integer, String> SCHEMA_TYPE_BY_PAGE_TYPE = new LinkedHashMap<Integer, String>() {{
        put(SitePage.SitePageType.PRODUCTS, "Product");
        put(SitePage.SitePageType.SOLUTIONS, "TechArticle");
        put(SitePage.SitePageType.NEWS, "NewsArticle");
        put(SitePage.SitePageType.SUCCESS_REFERENCE, "TechArticle");
        put(SitePage.SitePageType.BLOG, "BlogPosting");
    }};

    private ArticleAboutUtils() {
    }

    /**
     * 为文章类详情页 model 的 schemaJson 补充 about 结构化数据。
     *
     * @param model 页面渲染 model（来自 CMS API 响应）
     * @param host  当前请求归一化域名，用于菜单数据键控与绝对地址拼接
     */
    public static void fillAboutSchema(Map<String, Object> model, String host) {
        if (model == null || StringUtils.isBlank(host)) {
            return;
        }
        try {
            List<SitePage> menuPages = SiteMenuUtils.getMenuPages(host);
            if (menuPages.isEmpty()) {
                return;
            }
            List<Map<String,String>> aboutItems = new ArrayList<>();
            for (Map.Entry<String, String> field : RICH_TEXT_FIELDS.entrySet()) {
                Object entityObj = model.get(field.getKey());
                if (!(entityObj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> entity = (Map<String, Object>) entityObj;
                Object content = entity.get(field.getValue());
                if (!(content instanceof String) || StringUtils.isBlank((String) content)) {
                    continue;
                }
                collectLinks((String) content, menuPages, host, aboutItems);
            }
            if (aboutItems.isEmpty()) {
                return;
            }
            Object schemaObj = model.get("schemaJson");
            if (schemaObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> schemaJson = (Map<String, Object>) schemaObj;
                schemaJson.put("about", aboutItems);
            } else if (schemaObj instanceof String) {
                String schemaJsonStr = (String) schemaObj;
                JSONObject schemaJson = JSONObject.parseObject(schemaJsonStr);
                schemaJson.put("about", aboutItems);
                model.put("schemaJson", schemaJson.toJSONString());
            }
        } catch (Exception e) {
            // about 结构化数据生成失败不影响页面渲染
            log.warn("fill about schema failed, host:{}", host);
        }
    }

    /**
     * 从富文本正文提取指向产品/文章类菜单的站内 a 标签，并按菜单类型生成 about 条目。
     * <p>
     * URL 剥离语种前缀与查询/锚点后按首段匹配菜单 pageName（忽略大小写）定位所属类型；
     * 同 URL 只取首次出现，name 优先取 a 标签 title 属性，缺省取链接文本；
     * url 输出为 {@code https://{host}/…} 绝对地址。
     *
     * @param html       富文本正文（已经 sanitizeRichText 清理）
     * @param menuPages  当前域名菜单列表
     * @param host       当前域名
     * @param aboutItems 收集结果列表（追加写入）
     */
    private static void collectLinks(String html, List<SitePage> menuPages, String host,
                                     List<Map<String,String>> aboutItems) {
        Document document = Jsoup.parse(html);
        for (Element link : document.select("a[href]")) {
            String href = StringUtils.trimToEmpty(link.attr("href"));
            if (!href.contains(host)) {
                continue;
            }
            SitePage matchedMenu = matchMenuPage(href, menuPages);
            if (matchedMenu == null) {
                continue;
            }
            String schemaType = SCHEMA_TYPE_BY_PAGE_TYPE.get(matchedMenu.getPageType().intValue());
            if (schemaType == null) {
                continue;
            }
            Map<String,String> item = new LinkedHashMap<>();
            item.put("@type", schemaType);
            String name = StringUtils.isNotBlank(link.attr("title"))
                    ? link.attr("title").trim() : link.text().trim();
            item.put("name", name);
            item.put("url", href);
            aboutItems.add(item);
        }
    }

    /**
     * 校验 a 标签地址是否为当前域名的完整路径，命中则归一化为站内绝对路径
     * （以 / 开头，剥离查询串与锚点）。
     * <p>
     * 仅接受 http/https 协议下恰为当前 host 的完整地址（CMS 富文本下发口径）；
     * 相对路径、外域链接、锚点、mailto/tel/javascript 一律返回 null 跳过，不做处理。
     *
     * @param href a 标签 href 原始值
     * @param host 当前域名
     * @return 当前域名的站内绝对路径；其余情况返回 null
     */
    private static String resolveSitePath(String href, String host) {
        if (StringUtils.isBlank(href) || href.startsWith("#")
                || href.toLowerCase().startsWith("mailto:")
                || href.toLowerCase().startsWith("tel:")
                || href.toLowerCase().startsWith("javascript:")) {
            return null;
        }
        String lowerHref = href.toLowerCase();
        if (!lowerHref.startsWith("http://") && !lowerHref.startsWith("https://")) {
            return null;
        }
        String hostLower = host.toLowerCase();
        // 协议后、路径前必须恰为当前 host（允许结尾斜杠），否则视为外域
        String afterScheme = href.substring(lowerHref.indexOf("://") + 3);
        if (!afterScheme.toLowerCase().startsWith(hostLower)) {
            return null;
        }
        String tail = afterScheme.substring(hostLower.length());
        if (!tail.isEmpty() && tail.charAt(0) != '/' && tail.charAt(0) != '?'
                && tail.charAt(0) != '#') {
            // 子域形态（{host}.evil.com）不算同域
            return null;
        }
        String path = StringUtils.substringBefore(tail, "?");
        path = StringUtils.substringBefore(path, "#");
        if (path.isEmpty()) {
            return "/";
        }
        // 语种前缀页 URL 剥离前缀，与菜单匹配口径对齐
        for (String lang : SiteMenuUtils.getLangList(host)) {
            String langPrefix = "/" + lang + "/";
            if (path.startsWith(langPrefix)) {
                return path.substring(lang.length() + 1);
            }
            if (path.equals("/" + lang)) {
                return "/";
            }
        }
        return path;
    }

    /**
     * 按站点 URL 路径首段匹配所属菜单页。
     *
     * @param href      a 标签 href 原始值
     * @param menuPages 当前域名菜单列表
     * @return 命中的菜单页；无命中返回 null
     */
    private static SitePage matchMenuPage(String href, List<SitePage> menuPages) {
        for (SitePage menuPage : menuPages) {
            if (menuPage.getPageName() == null) {
                continue;
            }
            String menuName = menuPage.getPageName().trim();
            if (menuName.isEmpty()) {
                continue;
            }
            String prefix = menuName + "/";
            if (href.contains(prefix)) {
                return menuPage;
            }
        }
        return null;
    }
}
