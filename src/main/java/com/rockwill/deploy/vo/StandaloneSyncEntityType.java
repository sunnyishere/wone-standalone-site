package com.rockwill.deploy.vo;

import lombok.Getter;

@Getter
public enum StandaloneSyncEntityType {
    PRODUCT(SitePage.SitePageType.PRODUCTS),
    SOLUTION(SitePage.SitePageType.SOLUTIONS),
    NEWS(SitePage.SitePageType.NEWS),
    BLOG(SitePage.SitePageType.BLOG),
    PAGE(0), //菜单sitePage更新
    SUCCESS_CASE(SitePage.SitePageType.SUCCESS_REFERENCE);

    final int pageType;

    StandaloneSyncEntityType(int pageType) {
        this.pageType = pageType;
    }

}
