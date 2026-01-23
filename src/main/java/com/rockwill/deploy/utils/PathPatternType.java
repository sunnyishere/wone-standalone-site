package com.rockwill.deploy.utils;

public enum PathPatternType {
    // 匹配的路径类型
    SEARCH("/public/search"),
    LEAVE_MESSAGE("/public/leaveMessage"),
    DETAIL("/public/detail"),
    MULTI_LEVEL("/public/url"),

    CATEGORY_PAGINATION("/public/urlData"),
    CATEGORY_WITH_ID("/public/urlData"),
    
    MENU_WITH_PAGE("/menu/topNavPages"),
    MENU_WITHOUT_PAGE("/menu/topNavPages"),
    UPLOAD("/upload"),

    // 默认路径
    DEFAULT("/");
    
    private final String forwardTarget;
    
    PathPatternType(String forwardTarget) {
        this.forwardTarget = forwardTarget;
    }
    
    public String getForwardTarget() {
        return forwardTarget;
    }

    public String getName() {
        return name();
    }
    /**
     * 根据字符串模式类型获取对应的枚举值
     */
    public static PathPatternType fromString(String patternType) {
        if (patternType == null || patternType.trim().isEmpty()) {
            return DEFAULT;
        }
        
        try {
            return PathPatternType.valueOf(patternType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }

}