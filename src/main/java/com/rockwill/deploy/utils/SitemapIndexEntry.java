package com.rockwill.deploy.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 根 sitemap.xml 索引文件中的一个条目。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SitemapIndexEntry {

    /** 分片文件名，如 "sitemap-products.xml" */
    private String fileName;

    /** 该分片的最后修改时间 */
    private Date lastMod;
}
