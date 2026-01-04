package com.rockwill.deploy.service;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.render.TemplateEnginePageRenderer;
import com.rockwill.deploy.utils.*;
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
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
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
import java.util.concurrent.ConcurrentHashMap;
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

    Map<String, List<WebSitemapUrl>> webSitemapUrls = new ConcurrentHashMap<>();


    Map<String, List<String>> detailUrlNap = new ConcurrentHashMap<>();

    @Async("rockwillTaskExecutor")
    public void triggerGenPages(String domain) {
        detailUrlNap.put(domain, new CopyOnWriteArrayList<>());
        webSitemapUrls.put(domain, new CopyOnWriteArrayList<>());
        int state = 0;
        String reason = "";
        try {
            generateIndexPage(domain);
            knowledgeService.getSiteMenu(domain);
            String domainPath = staticOutputPath;
            if (!domain.equals(brandConfig.getDomain())) {
                domainPath = staticOutputPath + "/" + domain;
                new File(domainPath).mkdirs();
            }
            generateMenuAndDetailPage("", domain);
            copyStaticResources(domain);
            state = 2;
        } catch (Exception e) {
            log.error("triggerGenPages exception: {}", domain, e);
            reason =e.getMessage();
            state = 3;
        }
        Map<String, String> map = new HashMap<>();
        map.put("state", state + "");
        map.put("reason", reason);
        ResponseEntity<String> response = knowledgeService.submitForm("/publish/updateState", domain, map);
        log.info("主动发布更新发布状态：{},响应:{}", response.getStatusCode().value(), response.getBody());
    }

    /**
     * 生成所有静态页面
     * 生成sitemap
     * 生成robots
     *
     */
    public void generateAllPages(String domain) {
        if (ObjectUtils.isEmpty(domain)) {
            log.error("需提供独立部署域名配置: brand.domain");
            return;
        }
        detailUrlNap.put(domain, new CopyOnWriteArrayList<>());
        webSitemapUrls.put(domain, new CopyOnWriteArrayList<>());
        try {
            generateIndexPage(domain);
            knowledgeService.getSiteMenu(domain);
            String domainPath = staticOutputPath;
            if (!domain.equals(brandConfig.getDomain())) {
                domainPath = staticOutputPath + "/" + domain;
                new File(domainPath).mkdirs();
            }
            generateMenuAndDetailPage("", domain);
            for (String lang : SiteMenuUtils.getLangList()) {
                generateMenuAndDetailPage(lang, domain);
            }
            copyStaticResources(domain);

            siteSitemapUtils.generateStaticSitemap(domain, webSitemapUrls.get(domain));
            RobotsUtils.generateRobots(domain, isHttpsSupported(domain), domainPath);
        } catch (Exception e) {
            log.error("Failed to generating {} html files", domain, e);
        }
    }

    /**
     * 生成首页静态文件
     */
    public void generateIndexPage(String domain) {
        try {
            log.info("Start generating index  html files,site:{}", domain);
            String htmlContent = knowledgeService.getHome(jobRestTemplate, domain);
            if (ObjectUtils.isEmpty(htmlContent)) {
                log.info("Failed to request index content");
                return;
            }
            saveHtml(domain, "index", htmlContent);
            saveHtml(domain, "Home", htmlContent);
            addWebSitemap(htmlContent, "", 1.0, domain);
            log.info("End of generating index html files,site: {}", domain);
        } catch (Exception e) {
            log.error("generate index html files exception，site: {}", domain, e);
        }
    }


    /**
     * 菜单及详情页面
     */
    public void generateMenuAndDetailPage(String lang, String domain) {
        log.info("Start generating menu  html files,site:{},lang:{}", domain, lang);
        if (ObjectUtils.isEmpty(SiteMenuUtils.getMenuPages())) {
            log.error("Failed to request menu data: {}", domain);
            return;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (SitePage sitePage : SiteMenuUtils.getMenuPages()) {
            if (sitePage.getPageType() == SitePage.SitePageType.HOME
                    && ObjectUtils.isEmpty(lang)) {
                continue;
            }
            String pageName = sitePage.getPageName();
            if (!ObjectUtils.isEmpty(pageName)) {
                pageName = lang + "/" + pageName;
            }
            String menuPath = getApiPath(pageName);
            DomainHtmlVo domainHtmlVo = knowledgeService.getFromApi(jobRestTemplate, menuPath, domain);
            if (domainHtmlVo != null && !ObjectUtils.isEmpty(domainHtmlVo.getHtmlContent())) {
                saveHtml(domain, pageName, domainHtmlVo.getHtmlContent());
                addWebSitemap(domainHtmlVo.getHtmlContent(), "/" + pageName,
                        sitePage.getPageType() == SitePage.SitePageType.HOME ? 0.8 : 0.64, domain);
                if (sitePage.getPageType() == SitePage.SitePageType.HOME) {
                    //其他语种首页
                    saveHtml(domain, lang, domainHtmlVo.getHtmlContent());
                    addWebSitemap(domainHtmlVo.getHtmlContent(), "/" + lang, 0.8, domain);
                    continue;
                }
                List<CompletableFuture<Void>> pageTaskList = processPagination(sitePage, null, domainHtmlVo, false, lang, domain);
                if (!pageTaskList.isEmpty()) {
                    futures.addAll(pageTaskList);
                }
                if (sitePage.getPageType() == SitePage.SitePageType.PRODUCTS
                        || sitePage.getPageType() == SitePage.SitePageType.DOCUMENTS) {
                    CompletableFuture<Void> menuTask = CompletableFuture.runAsync(() -> {
                        processMenuCategory(sitePage, domainHtmlVo, lang, domain);
                    }, taskExecutor);
                    futures.add(menuTask);
                }
                if (sitePage.getPageType() != SitePage.SitePageType.DOCUMENTS
                        && sitePage.getPageType() != SitePage.SitePageType.PROFILE) {
                    CompletableFuture<Void> detailTask = CompletableFuture.runAsync(() -> {
                        processDetailPages(sitePage, domainHtmlVo, lang, domain);
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
        log.info("End of generating  menu html files,site: {},lang:{}", domain, lang);
    }

    private List<CompletableFuture<Void>> processPagination(SitePage sitePage, SitePage catePage, DomainHtmlVo domainHtmlVo, boolean isSubMenu, String lang, String domain) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        if (domainHtmlVo.getTotalPages() != null && domainHtmlVo.getTotalPages() > 1) {
            for (int p = 1; p <= domainHtmlVo.getTotalPages(); p++) {
                String menuName = sitePage.getPageName() + "-" + p;
                if (catePage != null) {
                    menuName = sitePage.getPageName() + "/" + catePage.getPageName() + "-" + catePage.getId() + "-" + p;
                }
                if (!ObjectUtils.isEmpty(lang)) {
                    menuName = lang + "/" + menuName;
                }
                DomainHtmlVo menuPageVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(menuName), domain);
                saveHtml(domain, menuName, menuPageVo.getHtmlContent());
                if (p != 1) {
                    addWebSitemap(menuPageVo.getHtmlContent(), "/" + menuName, 0.64, domain);
                }

                //仅对菜单根列表页面进行处理详情采集
                if (p >= 2 && !isSubMenu) {
                    if (sitePage.getPageType() != SitePage.SitePageType.DOCUMENTS
                            && sitePage.getPageType() != SitePage.SitePageType.PROFILE) {
                        CompletableFuture<Void> detailTask = CompletableFuture.runAsync(() -> {
                            processDetailPages(sitePage, menuPageVo, lang, domain);
                        }, taskExecutor);
                        futures.add(detailTask);
                    }
                }
            }
        }
        return futures;
    }


    /**
     * 生成页面菜单涉及到分类的页面
     *
     * @param sitePage     页面类型
     * @param domainHtmlVo 包含渲染后的html数据对象
     */
    public void processMenuCategory(SitePage sitePage, DomainHtmlVo domainHtmlVo, String lang, String domain) {
        log.info("Start generating menu category  html files,menu:{},lang:{}", sitePage.getPageName(), lang);
        // 分类
        if (domainHtmlVo.getModelMap() != null) {
            List<LinkedHashMap<String, Object>> categories = (List<LinkedHashMap<String, Object>>) domainHtmlVo.getModelMap().get("association");
            for (LinkedHashMap<String, Object> category : categories) {
                SitePage cate = new SitePage();
                cate.setPageName(category.get("name").toString().toLowerCase().trim());
                cate.setId(Long.parseLong(category.get("id").toString()));
                processSubCategory(sitePage, cate, lang, domain);
                if (category.containsKey("children")) {
                    List<LinkedHashMap<String, Object>> childs = (List<LinkedHashMap<String, Object>>) category.get("children");
                    if (!childs.isEmpty()) {
                        for (LinkedHashMap<String, Object> child : childs) {
                            SitePage sub = new SitePage();
                            sub.setPageName(child.get("name").toString().toLowerCase().trim());
                            sub.setId(Long.parseLong(child.get("id").toString()));
                            processSubCategory(sitePage, sub, lang, domain);
                        }
                    }
                }
            }
        }

        if (sitePage.getPageType() == SitePage.SitePageType.DOCUMENTS) {
            //资料 语言及排序分类页
            for (String sort : docFixedList) {
                String docName = sitePage.getPageName() + "/" + sort;
                if (!ObjectUtils.isEmpty(lang)) {
                    docName = lang + "/" + docName;
                }
                DomainHtmlVo subVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(docName), domain);
                saveHtml(domain, docName, subVo.getHtmlContent());
                addWebSitemap(subVo.getHtmlContent(), "/" + docName, 0.64, domain);
                SitePage sub = new SitePage();
                sub.setPageName(sort.substring(0, sort.lastIndexOf("-")));
                sub.setId(Long.parseLong(sort.substring(sort.lastIndexOf("-") + 1)));
                processPagination(sitePage, sub, subVo, true, lang, domain);
            }
        }
        log.info("End of generating menu category html files,menu: {}", sitePage.getPageName());
    }

    private void processSubCategory(SitePage menu, SitePage sitePage, String lang, String domain) {
        String docName = menu.getPageName() + "/" + sitePage.getPageName() + "-" + sitePage.getId();
        if (!ObjectUtils.isEmpty(lang)) {
            docName = lang + "/" + docName;
        }
        DomainHtmlVo categoryVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(docName), domain);
        saveHtml(domain, docName, categoryVo.getHtmlContent());
        addWebSitemap(categoryVo.getHtmlContent(), "/" + docName, 0.64, domain);
        processPagination(menu, sitePage, categoryVo, true, lang, domain);
    }

    public void processDetailPages(SitePage sitePage, DomainHtmlVo domainHtmlVo, String lang, String domain) {
        log.info("Start generating details html files,detail:{}", sitePage.getPageName());
        String cssQuery = "";
        if (sitePage.getPageType() == 3) {
            cssQuery = "div.am-u-sm-12 > a";
        } else if (sitePage.getPageType() == 2 || sitePage.getPageType() == 5 || sitePage.getPageType() == 7) {
            cssQuery = "li.dd-hover4 > a";
        }
        if (ObjectUtils.isEmpty(cssQuery)) {
            log.error("Currently, only products, news, solutions, and Success Reference are supported for static details.");
            return;
        }
        List<String> detailUrlList = getDetailLinkFromPage(domainHtmlVo.getHtmlContent(), cssQuery);
        for (String detailUrl : detailUrlList) {
            if (detailUrlNap.get(domain).contains(detailUrl)){
                continue;
            }
            detailUrlNap.get(domain).add(detailUrl);
            DomainHtmlVo subVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(detailUrl), domain);
            saveHtml(domain, detailUrl, subVo.getHtmlContent());
            addWebSitemap(subVo.getHtmlContent(), "/" + detailUrl, sitePage.getPageType() == 2 ? 0.9 : 0.8, domain);
            if (detailUrl.contains(sitePage.getPageName()+"/detail")) {
                if (subVo.getModelMap() != null && subVo.getModelMap().containsKey("modelList")) {
                    List<LinkedHashMap<String, Object>> modelList = (List<LinkedHashMap<String, Object>>) subVo.getModelMap().get("modelList");
                    for (LinkedHashMap<String, Object> model : modelList) {
                        String modelName = sitePage.getPageName()+subVo.getModelMap().get("suffix").toString().replace("/detail","")+ "-series" + model.get("id");
                        if (!ObjectUtils.isEmpty(lang)) {
                            modelName = lang + "/" + modelName;
                        }
                        DomainHtmlVo modelVo = knowledgeService.getFromApi(jobRestTemplate, getApiPath(modelName), domain);
                        saveHtml(domain, modelName, modelVo.getHtmlContent());
                    }
                }
            }
        }
        log.info("End of generating details html files,detail: {}", sitePage.getPageName());
    }

    private List<String> getDetailLinkFromPage(String htmlContent, String cssQuery) {
        Document document = Jsoup.parse(htmlContent);
        Elements directChildLinks = document.select(cssQuery);
        return directChildLinks.stream().map(element -> element.attr("href"))
                .filter(s -> s.startsWith("/")).map(link -> link.substring(1)).collect(Collectors.toList());
    }


    public void saveHtml(String domain, String namePrefix, String html) {
        if (ObjectUtils.isEmpty(html)) {
            log.info("Empty html content,uri:{}", namePrefix);
            return;
        }
        String fileName = sanitizeFileName(namePrefix) + ".html";
        String baseStaticPath = staticOutputPath;
        if (!domain.equals(brandConfig.getDomain())) {
            baseStaticPath += "/" + domain;
        }
        String siteDir = new File(baseStaticPath).getAbsolutePath();
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
    public void copyStaticResources(String domain) throws IOException {
        log.info("Start copying static resource files");
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:/static/**/*");
            if (resources.length == 0) {
                log.error("No static resource files were found under classpath:/static/");
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
                    String target = staticOutputPath;
                    if (!domain.equals(brandConfig.getDomain())) {
                        target += "/" + domain;
                    }
                    Path targetFile = Paths.get(target + "/static", resourcePath);
                    Files.createDirectories(targetFile.getParent());

                    // 特殊处理robots.txt和sitemap文件，复制到根目录
                    String filename = targetFile.getFileName().toString();
                    if ("robots.txt".equals(filename) || filename.startsWith("sitemap")) {
                        Path rootTarget = Paths.get(staticOutputPath,
                                domain.equals(brandConfig.getDomain()) ? "" : domain,
                                filename);
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

        log.error("Unable to resolve relative path to resource: {}", resourceDescription);
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


    private boolean isHttpsSupported(String domain) {
        try {
            String httpsUrl = "https://" + domain;
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

    private void addWebSitemap(String html, String uri, double priority, String domain) {
        if (ObjectUtils.isEmpty(html)) {
            return;
        }
        String baseUrl = "https://" + domain;
        String url = baseUrl + uri;
        try {
            WebSitemapUrl index = new WebSitemapUrl.Options(url)
                    .lastMod(new Date())
                    .priority(priority)
                    .changeFreq(ChangeFreq.DAILY)
                    .build();
            webSitemapUrls.get(domain).add(index);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}