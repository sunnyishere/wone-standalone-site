package com.rockwill.deploy.service;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.render.TemplateEnginePageRenderer;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.utils.SiteSitemapUtils;
import com.rockwill.deploy.utils.PathMatchUtils;
import com.rockwill.deploy.utils.RobotsUtils;
import com.rockwill.deploy.vo.DomainHtmlVo;
import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 静态页面生成服务
 * 使用TemplateEngine替代MockMvc实现页面静态化
 */
@Service
@Slf4j
public class StaticPageService {

    @Autowired
    TemplateEnginePageRenderer templateEnginePageRenderer;
    @Autowired
    RockwillKnowledgeService knowledgeService;
    @Autowired
    BrandConfig brandConfig;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SiteSitemapUtils siteSitemapUtils;

    @Autowired
    @Qualifier("jobRestTemplate")
    private RestTemplate jobRestTemplate;

    @Value("${static.output.path:${user.dir}/static-output}")
    private String staticOutputPath;

    static List<String> docFixedList = Arrays.asList("chinese-3686323", "english-3259826", "french-443636", "oldest-first-883325", "newest-first-632325");
    @Autowired
    @Qualifier("rockwillTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    List<WebSitemapUrl> webSitemapUrls = new CopyOnWriteArrayList<>();
    boolean isHttps;

    /**
     * 生成所有静态页面
     * 生成sitemap
     * 生成robots
     *
     */
    public void generateAllPages() {
        if (ObjectUtils.isEmpty(brandConfig.getDomain())) {
            log.warn("需提供独立部署域名配置: brand.domain");
            return;
        }
        webSitemapUrls.clear();
        try {
            generateIndexPage();
            generateMenuAndDetailPage();
            copyStaticResources();

            isHttps = isHttpsSupported();
            siteSitemapUtils.generateStaticSitemap(isHttps, webSitemapUrls);
            RobotsUtils.generateRobots(brandConfig.getDomain(), isHttps, staticOutputPath);
        } catch (Exception e) {
            log.error("Failed to generating {} html files", brandConfig.getDomain(), e);
        }
    }

    /**
     * 生成首页静态文件
     */
    public void generateIndexPage() {
        try {
            log.info("Start generating index  html files,site:{}", brandConfig.getDomain());
            String htmlContent = knowledgeService.getHome(jobRestTemplate);
            if (ObjectUtils.isEmpty(htmlContent)) {
                log.info("Failed to request index content");
                return;
            }
            saveHtml("index", htmlContent);
            saveHtml("Home", htmlContent);
            addWebSitemap(htmlContent, "", 1.0);
            addWebSitemap(htmlContent, "/Home", 1.0);
            log.info("End of generating index html files,site: {}", brandConfig.getDomain());
        } catch (Exception e) {
            log.error("generate index html files exception，site: {}", brandConfig.getDomain(), e);
        }
    }


    /**
     * 菜单及详情页面
     */
    public void generateMenuAndDetailPage() {
        log.info("Start generating menu  html files,site:{}", brandConfig.getDomain());
        List<SitePage> menuList = knowledgeService.getSiteMenu();
        if (ObjectUtils.isEmpty(menuList)) {
            log.warn("Failed to request menu data: {}", brandConfig.getDomain());
            return;
        }
        SiteMenuUtils.setMenuPages(menuList);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (SitePage sitePage : menuList) {
            if (sitePage.getPageType() == SitePage.SitePageType.HOME) {
                continue;
            }
            String menuPath = getApiPath(sitePage.getPageName());
            DomainHtmlVo domainHtmlVo = knowledgeService.getFromApi(jobRestTemplate, menuPath);
            if (domainHtmlVo != null && !ObjectUtils.isEmpty(domainHtmlVo.getHtmlContent())) {
                saveHtml(sitePage.getPageName(), domainHtmlVo.getHtmlContent());
                addWebSitemap(domainHtmlVo.getHtmlContent(), "/" + sitePage.getPageName(), 0.64);
                CompletableFuture<Void> pageTask = CompletableFuture.runAsync(() -> {
                    processPagination(sitePage, null, domainHtmlVo);
                }, taskExecutor);
                futures.add(pageTask);
                if (sitePage.getPageType() == SitePage.SitePageType.PRODUCTS
                        || sitePage.getPageType() == SitePage.SitePageType.DOCUMENTS) {
                    CompletableFuture<Void> menuTask = CompletableFuture.runAsync(() -> {
                        processMenuCategory(sitePage, domainHtmlVo);
                    }, taskExecutor);
                    futures.add(menuTask);
                }
                if (sitePage.getPageType() != SitePage.SitePageType.DOCUMENTS
                        && sitePage.getPageType() != SitePage.SitePageType.PROFILE) {
                    CompletableFuture<Void> detailTask = CompletableFuture.runAsync(() -> {
                        processDetailPages(sitePage, domainHtmlVo);
                    }, taskExecutor);
                    futures.add(detailTask);
                }
            }
        }
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allFutures.join();
        } catch (Exception e) {
            log.error("waiting menu and detail result exception", e);
        }
        log.info("End of generating  menu html files,site: {}", brandConfig.getDomain());
    }

    private void processPagination(SitePage sitePage, SitePage catePage, DomainHtmlVo domainHtmlVo) {
        if (domainHtmlVo.getTotalPages() != null && domainHtmlVo.getTotalPages() > 1) {
            for (int p = 1; p <= domainHtmlVo.getTotalPages(); p++) {
                String menuName = sitePage.getPageName() + "-" + p;
                if (catePage != null) {
                    menuName = sitePage.getPageName() + "/" + catePage.getPageName() + "-" + catePage.getId() + "-" + p;
                }
                DomainHtmlVo menuPageVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(menuName));
                saveHtml(menuName, menuPageVo.getHtmlContent());
                addWebSitemap(menuPageVo.getHtmlContent(), "/" + menuName, 0.64);
            }
        }
    }


    /**
     * 生成页面菜单涉及到分类的页面
     *
     * @param sitePage     页面类型
     * @param domainHtmlVo 包含渲染后的html数据对象
     */
    public void processMenuCategory(SitePage sitePage, DomainHtmlVo domainHtmlVo) {
        log.info("Start generating menu category  html files,menu:{}", sitePage.getPageName());
        // 分类
        if (domainHtmlVo.getModelMap() != null) {
            List<LinkedHashMap<String, Object>> categories = (List<LinkedHashMap<String, Object>>) domainHtmlVo.getModelMap().get("association");
            for (LinkedHashMap<String, Object> category : categories) {
                SitePage cate = new SitePage();
                cate.setPageName(category.get("name").toString().toLowerCase());
                cate.setId(Long.parseLong(category.get("id").toString()));
                processSubCategory(sitePage, cate);
                if (category.containsKey("children")) {
                    List<LinkedHashMap<String, Object>> childs = (List<LinkedHashMap<String, Object>>) category.get("children");
                    if (!childs.isEmpty()) {
                        for (LinkedHashMap<String, Object> child : childs) {
                            SitePage sub = new SitePage();
                            sub.setPageName(child.get("name").toString().toLowerCase());
                            sub.setId(Long.parseLong(child.get("id").toString()));
                            processSubCategory(sitePage, sub);
                        }
                    }
                }
            }
        }

        if (sitePage.getPageType() == SitePage.SitePageType.DOCUMENTS) {
            //资料 语言及排序分类页
            for (String sort : docFixedList) {
                String docName = sitePage.getPageName() + "/" + sort;
                DomainHtmlVo subVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(docName));
                saveHtml(docName, subVo.getHtmlContent());
                addWebSitemap(subVo.getHtmlContent(), "/" + docName, 0.64);
                SitePage sub = new SitePage();
                sub.setPageName(sort.substring(0,sort.lastIndexOf("-")));
                sub.setId(Long.parseLong(sort.substring(sort.lastIndexOf("-")+1)));
                processPagination(sitePage, sub, subVo);
            }
        }
        log.info("End of generating menu category html files,menu: {}", sitePage.getPageName());
    }

    private void processSubCategory(SitePage menu, SitePage sitePage) {
        String docName = menu.getPageName() + "/" + sitePage.getPageName() + "-" + sitePage.getId();
        DomainHtmlVo categoryVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(docName));
        saveHtml(docName, categoryVo.getHtmlContent());
        addWebSitemap(categoryVo.getHtmlContent(), "/" + docName, 0.64);
        processPagination(menu, sitePage, categoryVo);
    }

    public void processDetailPages(SitePage sitePage, DomainHtmlVo domainHtmlVo) {
        log.info("Start generating details html files,detail:{}", sitePage.getPageName());
        String cssQuery = "";
        if (sitePage.getPageType() == 3) {
            cssQuery = "div.am-u-sm-12 > a";
        } else if (sitePage.getPageType() == 2 || sitePage.getPageType() == 5 || sitePage.getPageType() == 7) {
            cssQuery = "li.dd-hover4 > a";
        }
        if (ObjectUtils.isEmpty(cssQuery)) {
            log.warn("Currently, only products, news, solutions, and Success Reference are supported for static details.");
            return;
        }
        List<String> detailUrlList = getDetailLinkFromPage(domainHtmlVo.getHtmlContent(), cssQuery);
        for (String detailUrl : detailUrlList) {
            DomainHtmlVo subVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(detailUrl));
            saveHtml(detailUrl, subVo.getHtmlContent());
            addWebSitemap(subVo.getHtmlContent(), "/" + detailUrl, sitePage.getPageType() == 2 ? 0.9 : 0.8);
            if (detailUrl.startsWith("Products/detail")) {
                processProdModelPages(subVo.getHtmlContent());
            }
        }
        log.info("End of generating details html files,detail: {}", sitePage.getPageName());
    }

    public void processProdModelPages(String productHtml) {
        if (ObjectUtils.isEmpty(productHtml)) {
            return;
        }
        List<String> modelUrlList = getDetailLinkFromPage(productHtml, "div.dd-scroll > div > a");
        for (String modelUrl : modelUrlList) {
            DomainHtmlVo subVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(modelUrl));
            saveHtml(modelUrl, subVo.getHtmlContent());
            addWebSitemap(subVo.getHtmlContent(), "/" + modelUrl, 0.9);
        }
    }

    private List<String> getDetailLinkFromPage(String htmlContent, String cssQuery) {
        Document document = Jsoup.parse(htmlContent);
        Elements directChildLinks = document.select(cssQuery);
        return directChildLinks.stream().map(element -> element.attr("href")).filter(s -> s.startsWith("/")).map(link -> link.substring(1)).collect(Collectors.toList());
    }


    public void saveHtml(String namePrefix, String html) {
        if (ObjectUtils.isEmpty(html)) {
            log.info("Empty html content,uri:{}", namePrefix);
            return;
        }
        String fileName = sanitizeFileName(namePrefix) + ".html";
        String siteDir = new File(staticOutputPath).getAbsolutePath();
        if (!"index.html".equals(fileName)) {
            String baseName = fileName.replace(".html", "");
            siteDir = new File(siteDir, baseName).getAbsolutePath();
            fileName = "index.html";
        }
        String menuPagePath = new File(siteDir, fileName).getAbsolutePath();
        try {
            templateEnginePageRenderer.saveToFile(html, menuPagePath);
        } catch (IOException e) {
            log.error("save {} html file exception", namePrefix, e);
        }
    }


    private String getApiPath(String name) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        return PathMatchUtils.matchResult(name).getForwardTarget();
    }

    /**
     * 复制静态资源文件到输出目录
     */
    public void copyStaticResources() throws IOException {
        log.info("Start copying static resource files");
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:/static/**/*");
            if (resources.length == 0) {
                log.warn("No static resource files were found under classpath:/static/");
                return;
            }
            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }

                try {
                    String resourcePath = getRelativeResourcePath(resource);
                    if (resourcePath == null) {
                        continue;
                    }
                    Path targetFile = Paths.get(staticOutputPath + "/static", resourcePath);
                    Files.createDirectories(targetFile.getParent());

                    // 特殊处理robots.txt和sitemap文件，复制到根目录
                    String filename = targetFile.getFileName().toString();
                    if ("robots.txt".equals(filename) || filename.startsWith("sitemap")) {
                        Path rootTarget = Paths.get(staticOutputPath, filename);
                        copyResourceToFile(resource, rootTarget);
                    } else {
                        copyResourceToFile(resource, targetFile);
                    }
                } catch (Exception e) {
                    log.error("An error occurred while copying resources: {}", resource.getDescription(), e);
                }
            }
            log.info("Static resource file copying completed");

        } catch (IOException e) {
            log.error("An IO exception occurred while searching for static resources.", e);
            throw e;
        }
    }

    /**
     * 从Resource对象中提取相对于classpath:/static/的路径
     */
    private String getRelativeResourcePath(Resource resource) throws IOException {
        String resourceDescription = resource.getURI().toString();
        if (resourceDescription.contains("!/")) {
            String pathAfterStatic = resourceDescription.substring(resourceDescription.indexOf("/static/") + 8);
            return java.net.URLDecoder.decode(pathAfterStatic, "UTF-8");
        } else {
            int staticIndex = resourceDescription.indexOf("/static/");
            if (staticIndex != -1) {
                return resourceDescription.substring(staticIndex + 8);
            }
        }

        log.warn("Unable to resolve relative path to resource: {}", resourceDescription);
        return null;
    }

    /**
     * 将Resource资源复制到指定的目标文件
     */
    private void copyResourceToFile(Resource resource, Path targetFile) throws IOException {
        try (InputStream inputStream = resource.getInputStream();
             OutputStream outputStream = Files.newOutputStream(targetFile)) {
            StreamUtils.copy(inputStream, outputStream);
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("\\s", "-").replaceAll("[\\\\:*?\"<>|]", "");
    }


    private boolean isHttpsSupported() {
        try {
            String httpsUrl = "https://" + brandConfig.getDomain();
            URL url = new URL(httpsUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private void addWebSitemap(String html, String uri, double priority) {
        if (ObjectUtils.isEmpty(html)) {
            return;
        }
        String baseUrl = (isHttps ? "https://" : "http://") + brandConfig.getDomain();
        String url = baseUrl + uri;
        try {
            WebSitemapUrl index = new WebSitemapUrl.Options(url)
                    .lastMod(new Date())
                    .priority(priority)
                    .changeFreq(ChangeFreq.DAILY)
                    .build();
            webSitemapUrls.add(index);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}