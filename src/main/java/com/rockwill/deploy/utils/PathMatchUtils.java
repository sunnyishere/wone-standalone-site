package com.rockwill.deploy.utils;


import com.rockwill.deploy.conf.ApplicationContextHolder;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.vo.SitePage;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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
        // 搜索规则: /search/categoryName-searchKey-categoryId-pageNum
        patterns.add(new PathPattern(
                Pattern.compile("^/search/(?<categoryName>.+?)-(?<searchKey>[^-]+)-(?<categoryId>\\d+)-(?<pageNum>\\d+)$"),
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

    public static MatchResult matchResult(String path) {
        MatchResult matchResult = new MatchResult();
        String normalizedPath = path;
        String lang = "";
        for (String sLang : SUPPORTED_LANGS) {
            if (normalizedPath.startsWith("/" + sLang)) {
                lang = sLang;
                normalizedPath=normalizedPath.substring(sLang.length()+1);
                break;
            }
        }
        if (normalizedPath.equals("/") || normalizedPath.equals("/Home")) {
            matchResult.setPatternType(PathPatternType.DEFAULT);
            if (ObjectUtils.isEmpty(lang)){
                matchResult.setForwardTarget("/");
            }else{
                matchResult.setForwardTarget("/home?lang=" + lang);
            }
            return matchResult;
        }
        //搜索时直接转发请求
        if (normalizedPath.startsWith("/search-")) {
            matchResult.setPatternType(PathPatternType.MENU_WITHOUT_PAGE);
            matchResult.setForwardTarget(PathPatternType.MENU_WITHOUT_PAGE.getForwardTarget() + "?name=" + normalizedPath.substring(1));
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
                    Optional<SitePage> optional = SiteMenuUtils.getMenuPages().stream()
                            .filter(sitePage -> sitePage.getPageName().toLowerCase()
                                    .equals(params.get("menuName")))
                            .findAny();
                    if (optional.isPresent()) {
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

    }
}
