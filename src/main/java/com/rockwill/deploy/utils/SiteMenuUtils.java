package com.rockwill.deploy.utils;

import com.rockwill.deploy.vo.SitePage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SiteMenuUtils {

    private static List<SitePage> menuPages = new ArrayList<>();

    public static List<SitePage> getMenuPages() {
        return menuPages;
    }

    public static void setMenuPages(List<SitePage> menuPages) {
        SiteMenuUtils.menuPages = menuPages;
    }

    public static List<String> getNameList() {
        return menuPages.stream().map(SitePage::getPageName).collect(Collectors.toList());
    }
}
