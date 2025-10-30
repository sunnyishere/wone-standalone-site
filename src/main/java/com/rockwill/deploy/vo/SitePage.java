package com.rockwill.deploy.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 页面管理对象 site_page
 *
 * @author ruoyi
 * @date 2025-06-09
 */
@Data
public class SitePage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键，自增ID */
    private Long id;

    /** 1=Home, 2=Products, 3=Solutions, 4=Documents, 5=News, 6=Profile */
    private Long pageType;

    /** 页面名称（可自定义修改） */
    private String pageName;

    /** 简短描述文字（用于卡片/摘要显示） */
    private String description;

    /** 详细内容 */
    private String detailedText;

    /** seo标题 */
    private String seoTitle;

    /** seo的内容 */
    private String seoContent;

    /** seo的关键字 */
    private String seoKeywords;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date created;

    /** 用户id */
    private Long uid;

    /** 字体颜色 */
    private String fontColor;

    /** 头部颜色 */
    private String headColor;

    /** 头部字体颜色 */
    private String headFontColor;

    /** 排序 */
    private Long sort;

    /** 是否显示 0不显示 1显示 */
    private Long isShow;

    /** 背景图 */
    private String icon;


    public static  class SitePageType{
        public static final int HOME=1;
        public static final int PRODUCTS=2;
        public static final int SOLUTIONS=3;
        public static final int DOCUMENTS=4;
        public static final int NEWS=5;
        public static final int PROFILE=6;
        public static final int SUCCESS_REFERENCE=7;
    }

}
