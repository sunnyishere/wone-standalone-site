package com.rockwill.deploy.utils;

import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 站点菜单数据持有（按域名键控）
 * <p>
 * 每个域名的 menuPages 与 langList 来自同一次 getMenu 接口响应，成组写入；
 * 多域名部署下各域名槽位隔离，互不覆盖（多租户隔离红线）。
 * 未知域名返回空列表并告警，由调用方既有判空逻辑兜底。
 */
@Slf4j
public class SiteMenuUtils {

    private static final Map<String, MenuData> MENU_DATA_MAP = new ConcurrentHashMap<>();

    /**
     * 成组写入某域名的菜单与语言列表（两者必须来自同一次 getMenu 响应）
     */
    public static void setMenuData(String domain, List<SitePage> menuPages, List<String> langList) {
        if (domain == null || domain.trim().isEmpty()) {
            log.warn("setMenuData with blank domain, skip");
            return;
        }
        MENU_DATA_MAP.put(domain, new MenuData(
                menuPages == null ? new ArrayList<>() : menuPages,
                langList == null ? new ArrayList<>() : langList));
    }

    public static List<SitePage> getMenuPages(String domain) {
        MenuData data = domain == null ? null : MENU_DATA_MAP.get(domain);
        if (data == null) {
            log.warn("menu data not loaded for domain: {}", domain);
            return new ArrayList<>();
        }
        return data.menuPages;
    }

    public static List<String> getLangList(String domain) {
        MenuData data = domain == null ? null : MENU_DATA_MAP.get(domain);
        if (data == null) {
            log.warn("menu data not loaded for domain: {}", domain);
            return new ArrayList<>();
        }
        return data.langList;
    }

    public static List<String> getNameList(String domain) {
        return getMenuPages(domain).stream().map(SitePage::getPageName).collect(Collectors.toList());
    }

    /**
     * 测试辅助：清空全部域名槽位
     */
    public static void clear() {
        MENU_DATA_MAP.clear();
    }

    private static class MenuData {
        private final List<SitePage> menuPages;
        private final List<String> langList;

        private MenuData(List<SitePage> menuPages, List<String> langList) {
            this.menuPages = menuPages;
            this.langList = langList;
        }
    }
}
