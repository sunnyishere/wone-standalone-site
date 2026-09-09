package com.rockwill.deploy.utils;

import com.redfin.sitemapgenerator.*;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 静态网站Sitemap生成
 * 支持按业务类型拆分 + 多语言 xhtml:link
 */
@Component
@Slf4j
public class SiteSitemapUtils {

    @Resource
    BrandConfig brandConfig;

    /** 分片阈值：每个 sitemap 分片最多包含的 URL 数量 */
    private static final int MAX_URLS_PER_SITEMAP = 1000;

    /** 业务类型 → 文件前缀映射（HOME 和 PROFILE 合并为 sitemap-pages） */
    private static final Map<Integer, String> PAGE_TYPE_PREFIX_MAP = new HashMap<>();

    static {
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.HOME, "sitemap-pages");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.PRODUCTS, "sitemap-products");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.SOLUTIONS, "sitemap-solutions");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.DOCUMENTS, "sitemap-documents");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.NEWS, "sitemap-news");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.PROFILE, "sitemap-pages");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.SUCCESS_REFERENCE, "sitemap-success-case");
        PAGE_TYPE_PREFIX_MAP.put(SitePage.SitePageType.BLOG, "sitemap-blog");
    }

    /**
     * 按业务类型生成 sitemap 分片文件和根索引文件。
     *
     * @param domain     域名
     * @param typeGroups 按 pageType 分组的 SitemapGroup 集合
     */
    public void generateStaticSitemap(String domain, Map<Integer, ? extends Map<String, SitemapGroup>> typeGroups) {
        log.info("generate {} Sitemap, type count:{}", domain, typeGroups.size());
        try {
            if (typeGroups == null || typeGroups.isEmpty()) {
                log.info("No sitemap groups for domain: {}", domain);
                return;
            }

            String path = brandConfig.getStaticOutput();
            if (!domain.equals(brandConfig.getDomain())) {
                path += "/" + domain;
            }
            File outputDirectory = new File(path);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            List<SitemapIndexEntry> indexEntries = new ArrayList<>();
            Set<String> processedPrefixes = new HashSet<>();

            for (Map.Entry<Integer, String> prefixEntry : PAGE_TYPE_PREFIX_MAP.entrySet()) {
                String prefix = prefixEntry.getValue();

                // 同一前缀只生成一次（如 HOME 和 PROFILE 共享 "sitemap-pages"）
                if (processedPrefixes.contains(prefix)) {
                    continue;
                }
                processedPrefixes.add(prefix);

                // 收集该前缀下所有 pageType 的 SitemapGroup
                List<SitemapGroup> groupList = new ArrayList<>();
                for (Map.Entry<Integer, String> innerEntry : PAGE_TYPE_PREFIX_MAP.entrySet()) {
                    if (!innerEntry.getValue().equals(prefix)) {
                        continue;
                    }
                    Map<String, SitemapGroup> groups = typeGroups.get(innerEntry.getKey());
                    if (groups != null && !groups.isEmpty()) {
                        groupList.addAll(groups.values());
                    }
                }

                if (groupList.isEmpty()) {
                    continue;
                }

                List<SitemapIndexEntry> entries = generateTypeSitemap(domain, prefix, groupList, outputDirectory);
                indexEntries.addAll(entries);
            }

            if (!indexEntries.isEmpty()) {
                generateSitemapIndex(domain, indexEntries, outputDirectory);
            }
        } catch (IOException e) {
            log.error("generating sitemap exception:{}", e.getMessage(), e);
        }
    }

    /**
     * 生成单个业务类型的 sitemap 分片。
     */
    private List<SitemapIndexEntry> generateTypeSitemap(String domain, String prefix,
                                                         List<SitemapGroup> groups, File outputDirectory)
            throws IOException {
        List<SitemapIndexEntry> entries = new ArrayList<>();
        int groupCount = groups.size();
        int numFiles = (int) Math.ceil((double) groupCount / MAX_URLS_PER_SITEMAP);
        boolean isMultiLang = SiteMenuUtils.getLangList().size() > 1;

        for (int i = 0; i < numFiles; i++) {
            int fromIndex = i * MAX_URLS_PER_SITEMAP;
            int toIndex = Math.min(fromIndex + MAX_URLS_PER_SITEMAP, groupCount);
            List<SitemapGroup> chunk = groups.subList(fromIndex, toIndex);

            String fileName;
            if (numFiles > 1) {
                fileName = prefix + "_" + (i + 1);
            } else {
                fileName = prefix;
            }

            List<File> generatedFiles;
            try {
                if (isMultiLang) {
                    GoogleLinkSitemapGenerator generator = GoogleLinkSitemapGenerator
                            .builder(getWebsiteUrl(domain), outputDirectory)
                            .fileNamePrefix(fileName)
                            .gzip(false)
                            .build();
                    List<GoogleLinkSitemapUrl> urls = new ArrayList<>();
                    for (SitemapGroup group : chunk) {
                        urls.add(buildGoogleLinkSitemapUrl(group, getWebsiteUrl(domain)));
                    }
                    generator.addUrls(urls);
                    generatedFiles = generator.write();
                } else {
                    WebSitemapGenerator generator = WebSitemapGenerator
                            .builder(getWebsiteUrl(domain), outputDirectory)
                            .fileNamePrefix(fileName)
                            .gzip(false)
                            .build();
                    List<WebSitemapUrl> urls = new ArrayList<>();
                    for (SitemapGroup group : chunk) {
                        urls.add(buildWebSitemapUrl(group, getWebsiteUrl(domain)));
                    }
                    generator.addUrls(urls);
                    generatedFiles = generator.write();
                }
            } catch (URISyntaxException e) {
                throw new IOException("Failed to build sitemap URL: " + e.getMessage(), e);
            }

            String xslUrl = getWebsiteUrl(domain) + "/sitemap_nb.xsl";
            for (File sitemapFile : generatedFiles) {
                addXslStylesheet(sitemapFile.getAbsolutePath(), xslUrl);
                entries.add(new SitemapIndexEntry(sitemapFile.getName(), new Date()));
            }
        }

        return entries;
    }

    /**
     * 从 SitemapGroup 构建带 xhtml:link 的 GoogleLinkSitemapUrl。
     */
    private GoogleLinkSitemapUrl buildGoogleLinkSitemapUrl(SitemapGroup group, String baseUrl)
            throws MalformedURLException, URISyntaxException {
        Map<String, Map<String, String>> alternates = new LinkedHashMap<>();

        // 找到默认语言的 URI 作为 <loc>
        String defaultUri = null;
        double maxPriority = 0;

        for (SitemapGroup.LangEntry entry : group.getLangEntries().values()) {
            String href = encodeUrl(baseUrl + entry.getUri());
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("hreflang", getLangFromEntry(entry));
            alternates.put(href, attrs);

            if (entry.isDefault()) {
                defaultUri = entry.getUri();
            }
            if (entry.getPriority() > maxPriority) {
                maxPriority = entry.getPriority();
            }
        }

        // 如果没有默认语言，使用第一个
        if (defaultUri == null && !group.getLangEntries().isEmpty()) {
            SitemapGroup.LangEntry first = group.getLangEntries().values().iterator().next();
            defaultUri = first.getUri();
        }

        String locUrl = encodeUrl(baseUrl + (defaultUri != null ? defaultUri : ""));

        GoogleLinkSitemapUrl.Options options = new GoogleLinkSitemapUrl.Options(locUrl, alternates);
        options.lastMod(group.getLastMod());
        options.priority(maxPriority);
        options.changeFreq(group.getChangeFreq());

        return options.build();
    }

    /**
     * 从 LangEntry 中获取 hreflang 属性值。
     * 如果 isDefault 为 true 则返回 "x-default"，否则返回 langCode。
     */
    private String getLangFromEntry(SitemapGroup.LangEntry entry) {
        if (entry.isDefault()) {
            return "x-default";
        }
        String langCode = entry.getLangCode();
        return langCode != null && !langCode.isEmpty() ? langCode : "";
    }

    /**
     * 对 URL 中的非法字符进行编码，与原始 WebSitemapUrl.Options 内部 new URL(url) 行为一致。
     */
    private String encodeUrl(String url) {
        try {
            return new java.net.URL(url).toString();
        } catch (MalformedURLException e) {
            log.warn("URL encoding failed for {}, falling back to raw value: {}", url, e.getMessage());
            return url;
        }
    }

    /**
     * 从 SitemapGroup 构建普通 WebSitemapUrl（单语言站点回退）。
     */
    private WebSitemapUrl buildWebSitemapUrl(SitemapGroup group, String baseUrl)
            throws MalformedURLException {
        // 使用默认语言的 URI
        String uri = "";
        double maxPriority = 0;
        for (SitemapGroup.LangEntry entry : group.getLangEntries().values()) {
            if (entry.isDefault()) {
                uri = entry.getUri();
            }
            if (entry.getPriority() > maxPriority) {
                maxPriority = entry.getPriority();
            }
        }
        if (uri.isEmpty() && !group.getLangEntries().isEmpty()) {
            uri = group.getLangEntries().values().iterator().next().getUri();
        }

        String locUrl = encodeUrl(baseUrl + uri);
        return new WebSitemapUrl.Options(locUrl)
                .lastMod(group.getLastMod())
                .priority(maxPriority)
                .changeFreq(group.getChangeFreq())
                .build();
    }

    /**
     * 生成根 sitemap.xml 索引文件。
     */
    private void generateSitemapIndex(String domain, List<SitemapIndexEntry> entries, File outputDirectory)
            throws IOException {
        File indexFile = new File(outputDirectory, "sitemap.xml");
        SitemapIndexGenerator indexGenerator = new SitemapIndexGenerator
                .Options(getWebsiteUrl(domain), indexFile)
                .build();

        for (SitemapIndexEntry entry : entries) {
            indexGenerator.addUrl(getWebsiteUrl(domain) + "/" + entry.getFileName(), entry.getLastMod());
        }
        indexGenerator.write();

        String xslUrl = getWebsiteUrl(domain) + "/sitemap_nb.xsl";
        addXslStylesheet(indexFile.getAbsolutePath(), xslUrl);
    }

    /**
     * 为Sitemap XML文件添加XSL样式表声明
     */
    private void addXslStylesheet(String sitemapFilePath, String xslStylesheetUrl) throws IOException {
        Path filePath = Paths.get(sitemapFilePath);
        List<String> lines = Files.readAllLines(filePath);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().startsWith("<?xml")) {
                lines.add(i + 1, "<?xml-stylesheet type=\"text/xsl\" href=\"" + xslStylesheetUrl + "\"?>");
                break;
            }
        }
        Files.write(filePath, lines);
    }

    String getWebsiteUrl(String domain) {
        return "https://" + domain;
    }
}