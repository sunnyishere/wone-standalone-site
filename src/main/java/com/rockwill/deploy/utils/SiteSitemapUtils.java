package com.rockwill.deploy.utils;

import com.redfin.sitemapgenerator.SitemapIndexGenerator;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.rockwill.deploy.conf.BrandConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 静态网站Sitemap生成
 *
 */
@Component
@Slf4j
public class SiteSitemapUtils {

    @Resource
    BrandConfig brandConfig;
    private final List<WebSitemapUrl> webSitemapUrlList = new ArrayList<>();
    private boolean isHttps;

    public void generateStaticSitemap(boolean isHttps, List<WebSitemapUrl> webSitemapUrls) {
        log.info("generate  Sitemap ,url size:{}", webSitemapUrls.size());
        this.isHttps = isHttps;
        webSitemapUrlList.clear();
        try {
            if (!webSitemapUrls.isEmpty()) {
                webSitemapUrlList.addAll(webSitemapUrls);
                webSitemapUrlList.sort((o1, o2) -> o2.getPriority().compareTo(o1.getPriority()));
                generateSitemapWithIndex();
            }
        } catch (IOException e) {
            log.error("generating sitemap exception:{}", e.getMessage(), e);
        }
    }

    /**
     * 生成分块Sitemap和索引文件
     */
    private void generateSitemapWithIndex()
            throws IOException {
        int maxUrlsPerSitemap = 2000;

        File outputDirectory = new File(brandConfig.getStaticOutput());
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        List<String> sitemapFileNames = new ArrayList<>();
        int urlCount = webSitemapUrlList.size();
        int numSitemaps = (int) Math.ceil((double) urlCount / maxUrlsPerSitemap);

        if (numSitemaps <= 1) {
            generateSitemap(brandConfig.getStaticOutput());
            return;
        }

        for (int i = 0; i < numSitemaps; i++) {
            int fromIndex = i * maxUrlsPerSitemap;
            int toIndex = Math.min(fromIndex + maxUrlsPerSitemap, urlCount);

            List<WebSitemapUrl> chunkUrls = webSitemapUrlList.subList(fromIndex, toIndex);
            String chunkFilename = "sitemap_" + (i + 1);

            WebSitemapGenerator chunkGenerator = WebSitemapGenerator
                    .builder(getWebsiteUrl(), outputDirectory)
                    .fileNamePrefix(chunkFilename)
                    .build();
            chunkGenerator.addUrls(chunkUrls);

            List<File> chunkFiles = chunkGenerator.write();
            if (!chunkFiles.isEmpty()) {
                sitemapFileNames.add(chunkFiles.get(0).getName());
            }
            String xslUrl = getWebsiteUrl() + "/sitemap_nb.xsl";
            for (File sitemapFile : chunkFiles) {
                addXslStylesheet(sitemapFile.getAbsolutePath(), xslUrl);
            }
        }

        File indexFile = new File(outputDirectory, "sitemap.xml");
        SitemapIndexGenerator indexGenerator = new SitemapIndexGenerator
                .Options(getWebsiteUrl(), indexFile)
                .build();

        for (String filename : sitemapFileNames) {
            indexGenerator.addUrl(getWebsiteUrl() + "/" + filename);
        }
        indexGenerator.write();
        String xslUrl = getWebsiteUrl() + "/sitemap_nb.xsl";
        addXslStylesheet(indexFile.getAbsolutePath(), xslUrl);

    }


    /**
     * 生成Sitemap XML文件
     *
     * @param outputDir 输出目录
     */
    private void generateSitemap(String outputDir) throws IOException {
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        WebSitemapGenerator sitemapGenerator = WebSitemapGenerator
                .builder(getWebsiteUrl(), outputDirectory)
                .fileNamePrefix("sitemap")
                .gzip(false)
                .build();

        sitemapGenerator.addUrls(webSitemapUrlList);
        List<File> sitemapFiles = sitemapGenerator.write();

        String xslUrl = getWebsiteUrl() + "/sitemap_nb.xsl";
        for (File sitemapFile : sitemapFiles) {
            addXslStylesheet(sitemapFile.getAbsolutePath(), xslUrl);
        }

        if (sitemapFiles.isEmpty()) {
            throw new IOException("未能生成Sitemap文件");
        }

    }


    /**
     * 为Sitemap XML文件添加XSL样式表声明
     *
     * @param sitemapFilePath  Sitemap XML文件的完整路径
     * @param xslStylesheetUrl XSL样式表文件的URL地址
     * @throws IOException
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

    String getWebsiteUrl() {
        return (isHttps ? "https://" : "http://") + brandConfig.getDomain();
    }

}