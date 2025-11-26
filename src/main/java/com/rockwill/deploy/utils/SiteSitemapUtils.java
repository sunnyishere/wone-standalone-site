package com.rockwill.deploy.utils;

import com.redfin.sitemapgenerator.SitemapIndexGenerator;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.rockwill.deploy.conf.BrandConfig;
import lombok.Setter;
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


    public void generateStaticSitemap(String domain, List<WebSitemapUrl> webSitemapUrls) {
        log.info("generate {} Sitemap ,url size:{}", domain, webSitemapUrls.size());
        try {
            if (!webSitemapUrls.isEmpty()) {
                List<WebSitemapUrl> webSitemapUrlList = new ArrayList<>(webSitemapUrls);
                webSitemapUrlList.sort((o1, o2) -> o2.getPriority().compareTo(o1.getPriority()));
                generateSitemapWithIndex(webSitemapUrlList, domain);
            }
        } catch (IOException e) {
            log.error("generating sitemap exception:{}", e.getMessage(), e);
        }
    }

    /**
     * 生成分块Sitemap和索引文件
     */
    private void generateSitemapWithIndex(List<WebSitemapUrl> webSitemapUrlList, String domain)
            throws IOException {
        int maxUrlsPerSitemap = 2000;

        String path = brandConfig.getStaticOutput();
        if (!domain.equals(brandConfig.getDomain())) {
            path += "/" + domain;
        }
        File outputDirectory = new File(path);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        List<String> sitemapFileNames = new ArrayList<>();
        int urlCount = webSitemapUrlList.size();
        int numSitemaps = (int) Math.ceil((double) urlCount / maxUrlsPerSitemap);

        if (numSitemaps <= 1) {
            generateSitemap(webSitemapUrlList, domain);
            return;
        }

        for (int i = 0; i < numSitemaps; i++) {
            int fromIndex = i * maxUrlsPerSitemap;
            int toIndex = Math.min(fromIndex + maxUrlsPerSitemap, urlCount);

            List<WebSitemapUrl> chunkUrls = webSitemapUrlList.subList(fromIndex, toIndex);
            String chunkFilename = "sitemap_" + (i + 1);

            WebSitemapGenerator chunkGenerator = WebSitemapGenerator
                    .builder(getWebsiteUrl(domain), outputDirectory)
                    .fileNamePrefix(chunkFilename)
                    .build();
            chunkGenerator.addUrls(chunkUrls);

            List<File> chunkFiles = chunkGenerator.write();
            if (!chunkFiles.isEmpty()) {
                sitemapFileNames.add(chunkFiles.get(0).getName());
            }
            String xslUrl = getWebsiteUrl(domain) + "/sitemap_nb.xsl";
            for (File sitemapFile : chunkFiles) {
                addXslStylesheet(sitemapFile.getAbsolutePath(), xslUrl);
            }
        }

        File indexFile = new File(outputDirectory, "sitemap.xml");
        SitemapIndexGenerator indexGenerator = new SitemapIndexGenerator
                .Options(getWebsiteUrl(domain), indexFile)
                .build();

        for (String filename : sitemapFileNames) {
            indexGenerator.addUrl(getWebsiteUrl(domain) + "/" + filename);
        }
        indexGenerator.write();
        String xslUrl = getWebsiteUrl(domain) + "/sitemap_nb.xsl";
        addXslStylesheet(indexFile.getAbsolutePath(), xslUrl);

    }


    /**
     * 生成Sitemap XML文件
     *
     * @param domain
     */
    private void generateSitemap(List<WebSitemapUrl> webSitemapUrlList, String domain) throws IOException {
        String path = brandConfig.getStaticOutput();
        if (!domain.equals(brandConfig.getDomain())) {
            path += "/" + domain;
        }
        File outputDirectory = new File(path);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        WebSitemapGenerator sitemapGenerator = WebSitemapGenerator
                .builder(getWebsiteUrl(domain), outputDirectory)
                .fileNamePrefix("sitemap")
                .gzip(false)
                .build();

        sitemapGenerator.addUrls(webSitemapUrlList);
        List<File> sitemapFiles = sitemapGenerator.write();

        String xslUrl = getWebsiteUrl(domain) + "/sitemap_nb.xsl";
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

    String getWebsiteUrl(String domain) {
        return "https://" + domain;
    }

}