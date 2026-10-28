package com.rockwill.deploy.vo;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 统一搜索 scope 枚举
 * <p>
 * {@link #code} 为搜索页 URL scope 参数与远程 /api/siteContent/search 接口的取值口径；
 * {@link #pageType} 映射到 {@link SitePage.SitePageType}，供搜索结果页类型 Tab
 * 经站点菜单（SiteMenuUtils 持有的菜单页）解析各语种 i18n 名称。
 * 常量声明顺序即搜索结果页类型 Tab 的展示顺序。
 */
@Getter
public enum SearchScope {

    ALL("all", null, "all", "All"),
    PROD("prod", (long) SitePage.SitePageType.PRODUCTS, "products", "Products"),
    ARTICLE_SOLUTION("article_solution", (long) SitePage.SitePageType.SOLUTIONS, "solutions", "Solutions"),
    SUCCESS_CASE("success_case", (long) SitePage.SitePageType.SUCCESS_REFERENCE, "case.studies", "Case Studies"),
    DOCUMENT("document", (long) SitePage.SitePageType.DOCUMENTS, "documents", "Documents"),
    BLOG("blog", (long) SitePage.SitePageType.BLOG, "blogs", "Blog");

    /** 索引 bizType / URL scope 值 */
    private final String code;
    /** 对应站点页面类型（SitePage.SitePageType 常量值，SitePage.pageType 为 Long）；ALL 无对应页面，为 null
     * -- GETTER --
     *
     * @return 对应站点页面类型常量值（与 SitePage.pageType 同为 Long）；ALL 返回 null
     */
    private final Long pageType;
    /** leading_dict(landing) 翻译 key；ALL 的标签取 advanced 字典的 all 键 */
    private final String dictKey;
    /** 菜单缺失或 i18n 名称不可用时的英文回退文案
     * -- GETTER --
     *
     * @return 菜单缺失或 i18n 名称不可用时的英文回退文案
     */
    private final String defaultLabel;

    SearchScope(String code, Long pageType, String dictKey, String defaultLabel) {
        this.code = code;
        this.pageType = pageType;
        this.dictKey = dictKey;
        this.defaultLabel = defaultLabel;
    }

    /**
     * 按当前语种取标签：landing 为 TopicDetail.publicDatails 注入的 landingDict.landing 子表
     *
     * @param landing 语种文案字典
     * @return scope 标签
     */
    public String label(Map<String, String> landing) {
        if (landing != null && dictKey != null && !dictKey.isEmpty()) {
            String value = landing.get(dictKey);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return defaultLabel;
    }

    /**
     * code → 枚举；未知值（含 null/空）回退 ALL
     *
     * @param code scope code 值
     * @return scope 枚举
     */
    public static SearchScope fromCode(String code) {
        if (code != null) {
            String trimmed = code.trim();
            for (SearchScope scope : values()) {
                if (scope.code.equals(trimmed)) {
                    return scope;
                }
            }
        }
        return ALL;
    }


    public static Set<String> allCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (SearchScope scope : values()) {
            codes.add(scope.code);
        }
        return codes;
    }

    /**
     * 判断是否为 ALL scope（全类型搜索）
     *
     * @param scope scope 枚举
     * @return true 表示 ALL（URL 与远程查询串中省略 scope 参数）
     */
    public static boolean isAll(SearchScope scope) {
        return scope == null || Objects.equals(scope.code, ALL.code);
    }

}
