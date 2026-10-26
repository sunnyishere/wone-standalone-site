package com.rockwill.deploy.utils;


import com.rockwill.deploy.conf.ApplicationContextHolder;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.vo.SitePage;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 对应二级域名相关页面路径匹配规则
 */
public class PathMatchUtils {
    private static final List<PathPattern> patterns = new ArrayList<>();
    private static final List<String> SUPPORTED_LANGS = loadSupportedLangs();

    // 初始化正则表达式模式，对应nginx的location规则
    static {
        // 搜索规则（新）: /search?keyword=xxx&cid=123&page=2，query 不参与路径匹配
        patterns.add(new PathPattern(
                Pattern.compile("^/search$"),
                PathPatternType.SEARCH
        ));

        // leaveMessage
        patterns.add(new PathPattern(Pattern.compile("/leaveMessage"), PathPatternType.LEAVE_MESSAGE));

        // 详情页 /menuName/detail/title-id
        patterns.add(new PathPattern(
                Pattern.compile("^/(?<menuName>[a-zA-Z0-9_%\\-]+)/detail/(?<title>.+?)-(?<id>\\d+)$"),
                PathPatternType.DETAIL
        ));

        // 分类分页 /menuName/docName-id-pageNum
        patterns.add(new PathPattern(
                Pattern.compile("^/(?<menuName>[a-zA-Z0-9_%\\-]+)/(?!detail/)(?<docName>.+?)-(?<id>\\d+)(?:-(?<pageNum>\\d+))?$"),
                PathPatternType.CATEGORY_PAGINATION
        ));

        // 多级路径 /menuName/xxx-id
        patterns.add(new PathPattern(
                Pattern.compile("^/(?<menuName>[a-zA-Z0-9_%\\-]+)/([^/]+)-(?<id>[a-zA-Z0-9]+)$"),
                PathPatternType.MULTI_LEVEL
        ));

        // 分类带ID /menuName/docName/id-num
        patterns.add(new PathPattern(
                Pattern.compile("^/(?<menuName>[a-zA-Z0-9_%\\-]+)/(?<docName>[^/]+)/(?<id>[a-zA-Z0-9_%\\-]+)-(?<pageNum>\\d+)$"),
                PathPatternType.CATEGORY_WITH_ID
        ));


        // 带页码菜单 /menuName-pageNum
        patterns.add(new PathPattern(
                Pattern.compile("^/(?<menuName>[a-zA-Z0-9_%.\\-]+)-(?<pageNum>\\d+)$"),
                PathPatternType.MENU_WITH_PAGE
        ));

        // 无页码菜单，包括search /menuName
        patterns.add(new PathPattern(
                Pattern.compile("^/(?<menuName>[a-zA-Z0-9_%\\-]+)$"),
                PathPatternType.MENU_WITHOUT_PAGE
        ));

    }

    static class PathPattern {
        Pattern regex;
        PathPatternType type;

        PathPattern(Pattern regex, PathPatternType type) {
            this.regex = regex;
            this.type = type;
        }
    }

    private static List<String> loadSupportedLangs() {
        BrandConfig appConfig = ApplicationContextHolder.getBean(BrandConfig.class);
        return new ArrayList<>(appConfig.getLanguages());
    }

    public static MatchResult matchResult(String path, String domain) {
        MatchResult matchResult = new MatchResult();
        String normalizedPath = path;
        String lang = "";
        for (String sLang : SUPPORTED_LANGS) {
            if (normalizedPath.startsWith("/" + sLang)) {
                lang = sLang;
                normalizedPath=normalizedPath.substring(sLang.length()+1);
                if (normalizedPath.isEmpty()) {
                    normalizedPath = "/";
                }
                break;
            }
        }
        if (normalizedPath.equals("/") || normalizedPath.equals("/Home")) {
            matchResult.setPatternType(PathPatternType.MENU_WITHOUT_PAGE);
            if (ObjectUtils.isEmpty(lang)){
                matchResult.setForwardTarget("/");
            }else{
                matchResult.setForwardTarget("/home?lang=" + lang);
            }
            return matchResult;
        }
        if (normalizedPath.equals("/upload")) {
            matchResult.setPatternType(PathPatternType.UPLOAD);
            matchResult.setForwardTarget("/upload");
            return matchResult;
        }
        //搜索时直接转发请求
        if (normalizedPath.startsWith("/search-") || normalizedPath.startsWith("/search/")) {
            // 旧搜索 URL（/search-关键词、/search/分类-关键词-分类ID-页码）已改为
            // /search?keyword=&cid=&page= 查询参数形态，此处 301 到新形态保住已收录链接
            matchResult.setPatternType(PathPatternType.SEARCH);
            matchResult.setRedirect(true);
            matchResult.setForwardTarget(buildSearchRedirect(normalizedPath.substring(1), lang));
            return matchResult;
        }
        for (PathPattern pattern : patterns) {
            Matcher matcher = pattern.regex.matcher(normalizedPath);
            if (matcher.matches()) {
                matchResult.setPatternType(pattern.type);
                Map<String, String> params = extractNamedGroups(matcher);
                if (pattern.type == PathPatternType.CATEGORY_WITH_ID
                        || pattern.type == PathPatternType.CATEGORY_PAGINATION) {
                    if (!params.containsKey("pageNum")) {
                        params.put("pageNum", "1");
                    }
                }
                if (pattern.type == PathPatternType.MULTI_LEVEL) {
                    String id = params.get("id");
                    if (!id.startsWith("series") && !NumberUtils.isCreatable(id)) {
                        matchResult.setIllegal(true);
                    }
                }
                if (params.get("menuName") != null) {
                    boolean isLegal =  SiteMenuUtils.getMenuPages(domain).stream()
                            .filter(sitePage -> sitePage.getPageName() != null)
                            .anyMatch(sitePage -> sitePage.getPageName()
                                    .equalsIgnoreCase(params.get("menuName").trim()));
                    if (!isLegal) {
                        matchResult.setIllegal(true);
                    }
                }
                if (!ObjectUtils.isEmpty(lang)) {
                    params.put("lang", lang);
                }
                String target = pattern.type.getForwardTarget() + "?" + buildGetParams(params);
                if (target.endsWith("?")) {
                    target = target.substring(0, target.length() - 1);
                }
                if (pattern.type == PathPatternType.MENU_WITH_PAGE
                        || pattern.type == PathPatternType.MENU_WITHOUT_PAGE) {
                    target = target.replace("menuName", "name");
                }
                matchResult.setForwardTarget(target);
                break;
            }
        }
        return matchResult;
    }

    /**
     * 将旧搜索 URL 重构为查询参数形态的 301 目标。
     * 兼容两种旧形态：
     * <ul>
     *   <li>{@code search-关键词}（普通搜索，整段为关键词，页码固定第 1 页）</li>
     *   <li>{@code search/分类名-关键词-分类ID-页码}（分类搜索，与旧路由正则同口径切分：
     *       末两段为纯数字的 ID 与页码，紧邻 ID 的短横线段为关键词 {@code [^-]+}，
     *       其余前缀为分类名丢弃）</li>
     * </ul>
     *
     * @param oldPath 去掉语言前缀后的旧搜索路径（如 search-abc、search/cat-abc-12-2）
     * @param lang 站点语言短码（空为英语，目标链接不带前缀）
     * @return 形如 /search?keyword=abc&cid=12&page=2 的重定向目标；page=1 时省略 page 参数
     */
    private static String buildSearchRedirect(String oldPath, String lang) {
        String body = oldPath.startsWith("search-")
                ? oldPath.substring("search-".length())
                : oldPath.substring("search/".length());
        String keyword = body;
        String cid = null;
        String page = null;
        if (oldPath.startsWith("search-")) {
            // 普通搜索旧形态无页码（页码固定第 1 页）
            page = "1";
        } else {
            // 分类搜索：按旧正则口径切分 search/分类名-关键词-分类ID-页码
            java.util.regex.Matcher m =
                    Pattern.compile("^(.+)-([^-]+)-(\\d+)-(\\d+)$").matcher(body);
            if (m.matches()) {
                keyword = m.group(2);
                cid = m.group(3);
                page = m.group(4);
            }
        }
        StringBuilder target = new StringBuilder(ObjectUtils.isEmpty(lang) ? "/search" : "/" + lang + "/search");
        target.append("?keyword=").append(urlEncodeUtf8(keyword));
        if (cid != null) {
            target.append("&cid=").append(cid);
        }
        if (page != null && !"1".equals(page)) {
            target.append("&page=").append(page);
        }
        return target.toString();
    }

    /**
     * UTF-8 百分号编码（空值安全），供重定向目标拼接查询参数
     */
    private static String urlEncodeUtf8(String value) {
        try {
            return java.net.URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
        } catch (Exception e) {
            return StringUtils.defaultString(value);
        }
    }

    private static String buildGetParams(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .map(entry ->
                        entry.getKey() + "=" + entry.getValue()
                )
                .collect(Collectors.joining("&"));
    }

    private static Map<String, String> extractNamedGroups(Matcher matcher) {
        Map<String, String> params = new HashMap<>();
        try {
            for (String groupName : Arrays.asList("menuName", "docName", "title", "id", "pageNum", "categoryName", "searchKey", "categoryId")) {
                try {
                    String value = matcher.group(groupName);
                    if (value != null) {
                        params.put(groupName, value);
                    }
                } catch (IllegalArgumentException e) {
                    // 分组不存在，忽略
                }
            }
        } catch (Exception e) {
            // 处理分组提取异常
        }
        return params;
    }

    @Data
    public static class MatchResult {
        PathPatternType patternType;
        String forwardTarget;
        Boolean illegal=false;
        /**
         * true 时 forwardTarget 不是后端转发目标而是 301 目标地址
         * （旧 URL 兼容迁移，如旧搜索路径 → /search?keyword=...），
         * 拦截器识别后直接重定向，不再进入 Controller
         */
        boolean redirect;

    }
}
