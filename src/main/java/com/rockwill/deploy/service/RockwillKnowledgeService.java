package com.rockwill.deploy.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.conf.GoogleTagProperties;
import com.rockwill.deploy.conf.SiteContentProperties;
import com.rockwill.deploy.render.TemplateEnginePageRenderer;
import com.rockwill.deploy.utils.ArticleAboutUtils;
import com.rockwill.deploy.utils.RichTextUtils;
import com.rockwill.deploy.utils.SignUtils;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.utils.ThymeleafUtils;
import com.rockwill.deploy.vo.AjaxResult;
import com.rockwill.deploy.vo.DomainHtmlVo;
import com.rockwill.deploy.vo.SearchScope;
import com.rockwill.deploy.vo.SitePage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 品牌商服务接口请求
 * <p>
 * 1.提供静态化页面接口
 * 2.渲染内容请求
 */
@Service
@Slf4j
public class RockwillKnowledgeService {
    @Value("${spring.profiles.active:prod}")
    String devMode;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    GoogleTagProperties googleTagProperties;

    @Resource
    BrandConfig brandConfig;

    @Resource
    SiteContentProperties siteContentProperties;

    @Resource
    SiteCommonContextCache siteCommonContextCache;

    @Resource
    TemplateEnginePageRenderer templateEnginePageRenderer;

    Map<String, String> languageMap = new HashMap<String, String>() {{
        put("en", "english");
        put("fr", "french");
        put("pl", "polish");
        put("et", "estonian");
        put("bn", "bengali");
        put("id", "indonesian");
        put("uk", "ukrainian");
        put("cs", "czech");
        put("kn", "kannada");
        put("gl", "galliccian");
        put("pa", "punjabi");
        put("sv", "swedish");
        put("pt", "portuguese");
        put("no", "norwegian");
        put("ar", "arabic");
        put("ku", "kurde");
        put("hr", "croatian");
        put("fi", "finnish");
        put("eu", "basque");
        put("vi", "vietnamese");
        put("tr", "turkish");
        put("sw", "swahili");
        put("ja", "japanese");
        put("is", "icelandic");
        put("lv", "latvian");
        put("nl", "dutch");
        put("de", "german");
        put("hi", "hindi");
        put("it", "italian");
        put("hy", "armenian");
        put("es", "spanish");
        put("te", "telugu");
        put("eo", "esperanto");
        put("ka", "georgian");
        put("ru", "russian");
        put("fa", "persian");
        put("uz", "uzbek");
        put("sl", "slovenian");
        put("ca", "catalan");
        put("bg", "bulgarian");
        put("hu", "hungarian");
        put("el", "greek");
        put("he", "hebrew");
        put("sr", "serbian");
        put("ps", "pashto");
        put("ne", "nepali");
        put("kk", "kazakh");
        put("az", "Azerbaijani");
        put("af", "Afrikaans");
        put("ms", "Malay");
        put("la", "Latin");
        put("ko", "Korean");
        put("mk", "Macedonian");
        put("mt", "Maltese");
        put("tl", "Tagalog");
        put("ur", "Urdu");
        put("ta", "Tamil");
        put("si", "Sinhalese");
        put("ha", "Hausa");
        put("da", "Danish");
        put("ga", "Irish");
        put("ceb", "Cebuano");
        put("th", "Thai");
    }};

    @Value("${wcm-api}")
    private String wcmApi;

    /**
     * 发往知识库（wcm-api）请求的全局并发闸门：静态化线程池的任务除了远程请求还包含
     * 本地渲染与文件写入，压池会一并拖慢本地工作，故仅对 HTTP 调用限流。
     * 许可数来自 brand.wcmApiConcurrency（未配置默认 5），见 initWcmApiSemaphore。
     */
    private Semaphore wcmApiSemaphore;

    @PostConstruct
    void initWcmApiSemaphore() {
        wcmApiSemaphore = new Semaphore(brandConfig.resolveWcmApiConcurrency());
    }

    /**
     * 在全局 wcm-api 并发许可内执行远程调用：获取许可阻塞等待（静态化为后台任务，可接受排队），
     * 异常时在 finally 释放，调用方异常语义不变。
     */
    <T> T callWithWcmApiPermit(Supplier<T> call) {
        try {
            wcmApiSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for wcm-api permit", e);
        }
        try {
            return call.get();
        } finally {
            wcmApiSemaphore.release();
        }
    }


    @Value("${cdn.enabled:true}")
    private boolean cdnEnabled;

    @Value("${cdn.prefix:https://oss.iee-business.com}")
    private String cdnPrefix;
    @Value("${cdn.version}")
    private String version;
    @Value("${landingBaseUrl:https://www.iee-business.com}")
    private String landingBaseUrl;

    /**
     * 查询网页菜单
     *
     * @return 菜单列表
     */
    public List<SitePage> getSiteMenu(String domain) {
        log.info("request site menu data");
        try {
            ParameterizedTypeReference<AjaxResult<JSONObject>> typeReference =
                    new ParameterizedTypeReference<AjaxResult<JSONObject>>() {
                    };
            HttpHeaders headers = new HttpHeaders();
            headers.add("Deploy-Domain", domain);
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<AjaxResult<JSONObject>> response = callWithWcmApiPermit(() -> restTemplate.exchange(
                    wcmApi + "getMenu",
                    HttpMethod.GET,
                    requestEntity,
                    typeReference
            ));
            AjaxResult<JSONObject> ajaxResult = response.getBody();
            if (ajaxResult != null) {
                if (ajaxResult.getCode() == 200) {
                    List<SitePage> sitePageList = JSON.parseArray(ajaxResult.getData().getJSONArray("sitePages")
                            .toString(), SitePage.class);
                    List<String> langList = JSON.parseArray(ajaxResult.getData().getJSONArray("langList")
                            .toString(), String.class);
                    SiteMenuUtils.setMenuData(domain, sitePageList, langList);
                    log.info("lang list:{}", String.join(",", langList));
                    return sitePageList;
                }
                log.error("request resp code:{},msg:{}", ajaxResult.getCode(), ajaxResult.getMsg());
            }

        } catch (Exception e) {
            log.error("request site menu exception", e);
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    /**
     * 请求品牌商服务接口
     *
     * @param path 请求api路径
     * @return 返回html内容
     */
    public DomainHtmlVo getFromApi(RestTemplate restTemplate, String path, String host) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        final String apiPath = path;
        log.info("request wcm  {} ", apiPath);
        try {
            ParameterizedTypeReference<AjaxResult<Map<String, Object>>> typeReference =
                    new ParameterizedTypeReference<AjaxResult<Map<String, Object>>>() {
                    };
            HttpHeaders headers = new HttpHeaders();
            headers.add("Deploy-Domain", host);
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<AjaxResult<Map<String, Object>>> responseEntity = callWithWcmApiPermit(() -> restTemplate.exchange(
                    wcmApi + apiPath,
                    HttpMethod.GET,
                    requestEntity,
                    typeReference
            ));
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                AjaxResult<Map<String, Object>> result = responseEntity.getBody();
                Map<String, Object> model = result.getData();
                if (model != null) {
                    if (path.contains("Blog")
                            && model.containsKey("pageData")) {
                        Object pageData = model.get("pageData");
                        if (pageData != null) {
                            Map<String, Object> pageDataMap = (Map<String, Object>) pageData;
                            Object rows = pageDataMap.get("rows");
                            if (rows instanceof List) {
                                List<Map<String, Object>> rowList = (List<Map<String, Object>>) rows;
                                for (Map<String, Object> row : rowList) {
                                    if (!row.containsKey("slugTitle") || row.get("slugTitle") == null) {
                                        Object slugUrl = row.get("slugUrl");
                                        row.put("slugTitle", slugUrl);
                                    }
                                }
                            }
                        }
                    }
                    fillCommonModel(model, host);
                    handleDateKey(model);
                    handleLibraryFileSize(model);
                    sanitizeRichText(model);
                    ArticleAboutUtils.fillAboutSchema(model, host);
                    upgradeOssAssetsToHttps(model);
                    if (model.containsKey("prodFaqList")) {
                        model.put("pageFaqList", model.get("prodFaqList"));
                    }
                    String websiteUrl = "";
                    if (model.containsKey("websitePath")) {
                        String websitePath = model.get("websitePath").toString();
                        model.put("indexPath", websitePath.substring(0, websitePath.length() - 1));
                    }
                    if (model.containsKey("suffix")) {
                        Object suffix = model.get("suffix");
                        Object pageName = model.get("pageName");
                        Object website = model.get("websitePath");
                        if (model.containsKey("currentLang")
                                && model.get("currentLang") != null && !model.get("currentLang")
                                .equals("en")) {
                            website += model.get("currentLang") + "/" + pageName + suffix;
                        } else {
                            website += "" + pageName + suffix;
                        }
                        websiteUrl = website.toString();
                        model.put("websiteUrl", website);
                    }
                    handlePlatformSuccessCaseLink(model);
                    handleSchemaJson(model, websiteUrl, host);
                    if (!model.isEmpty()) {
                        String templateName = model.get("templateName").toString();
                        DomainHtmlVo domainHtmlVo = new DomainHtmlVo();
                        if (StringUtils.isNotEmpty(templateName)) {
                            ThymeleafUtils.fillChildCategoryCardSlugUrls(model);
                            String content = templateEnginePageRenderer.renderPage(templateName, model);
                            if ("products-details".equals(templateName) && content != null) {
                                content = content.replace("&amp;", "&");
                            }
                            domainHtmlVo.setHtmlContent(content);
                            domainHtmlVo.setModelMap(model);
                            if (model.containsKey("pageData")) {
                                Map<String, Object> pageMap = (Map<String, Object>) model.get("pageData");
                                domainHtmlVo.setTotalPages(Integer.parseInt(pageMap.get("totalPages").toString()));
                            }
                            if (templateName.equals("404")){
                                domainHtmlVo.setHttpErrCode(HttpStatus.NOT_FOUND.value());
                            }
                            // 渲染成功后收割通用上下文（渲染完成态快照，供搜索页合并）
                            harvestCommonContext(model, host);
                        }
                        return domainHtmlVo;
                    }
                } else {
                    log.error("request resp code:{},msg:{}", result.getCode(), result.getMsg());
                    DomainHtmlVo domainHtmlVo = new DomainHtmlVo();
                    domainHtmlVo.setHttpErrCode(result.getCode());
                    return domainHtmlVo;
                }
            }
            log.error("request {} error: {}", path, responseEntity.getStatusCode());
        } catch (Exception e) {
            DomainHtmlVo domainHtmlVo = new DomainHtmlVo();
            log.error("request {} exception", path, e);
            if (e instanceof HttpClientErrorException.NotFound){
                domainHtmlVo.setHttpErrCode(HttpStatus.NOT_FOUND.value());
            }
            return domainHtmlVo;
        }
        return new DomainHtmlVo();
    }

    /**
     * 富文本字段定点清单：model 中的实体 key → [内容字段名, img alt 兜底字段名]。
     * prod 的内容字段为 detail、alt 取 name；news/article/successCase/blog 的内容字段
     * 均为 content、alt 取 title。
     */
    private static final Map<String, String[]> RICH_TEXT_FIELDS = new LinkedHashMap<String, String[]>() {{
        put("prod", new String[]{"detail", "name"});
        put("news", new String[]{"content", "title"});
        put("article", new String[]{"content", "title"});
        put("successCase", new String[]{"content", "title"});
        put("blog", new String[]{"content", "title"});
        put("library", new String[]{"content", "name"});
    }};
    private static final String[] FAQ_LIST_KEYS = {"prodFaqList", "pageFaqList"};

    /**
     * 定点清理富文本字段：剥离被粘贴的 HTML 文档骨架（{@code <!DOCTYPE>/<html>/<head>/<body>}），
     * 并为缺失 alt 的 img 补齐兜底 alt（prod 取实体 name，其余取实体 title）。
     * <p>
     * CMS 编辑器粘贴网页源码时会把整份文档存进正文，经 {@code th:utext} 原样输出会让页面
     * 出现第二套 head/body；富文本内 UEditor 图片普遍缺 alt。剥离与补齐规则分别见
     * {@link RichTextUtils#stripDocumentSkeleton(String)} 与
     * {@link RichTextUtils#fillMissingAlt(String, String)}。
     *
     * @param model 页面渲染 model
     */
    private void sanitizeRichText(Map<String, Object> model) {
        for (Map.Entry<String, String[]> field : RICH_TEXT_FIELDS.entrySet()) {
            String[] fieldNames = field.getValue();
            Object entityObj = model.get(field.getKey());
            if (!(entityObj instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> entity = (Map<String, Object>) entityObj;
            Object content = entity.get(fieldNames[0]);
            if (!(content instanceof String) || StringUtils.isBlank((String) content)) {
                continue;
            }
            Object alt = entity.get(fieldNames[1]);
            String altText = alt instanceof String && StringUtils.isNotBlank((String) alt) ? (String) alt : null;
            String cleaned = RichTextUtils.stripDocumentSkeleton((String) content);
            cleaned = RichTextUtils.fillMissingAlt(cleaned, altText);
            cleaned = RichTextUtils.upgradeOssToHttps(cleaned);
            entity.put(fieldNames[0], cleaned);
        }
        for (String faqKey : FAQ_LIST_KEYS) {
            Object faqListObj = model.get(faqKey);
            if (!(faqListObj instanceof List)) {
                continue;
            }
            for (Object itemObj : (List<?>) faqListObj) {
                if (!(itemObj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) itemObj;
                Object content = item.get("content");
                if (!(content instanceof String) || StringUtils.isBlank((String) content)) {
                    continue;
                }
                String cleaned = RichTextUtils.stripDocumentSkeleton((String) content);
                item.put("content", cleaned);
            }
        }
    }

    /**
     * 递归升级 model 各层字符串中白名单 OSS host 的 http 资源地址为 https，
     * 消除富文本图片、library.icon、列表 item.icon 等下发内容造成的混合内容。
     * 替换规则见 {@link RichTextUtils#upgradeOssToHttps(String)}，仅命中
     * {@code http://oss.iwone.cn} 与 {@code http://oss.iee-business.com} 两个前缀，
     * 其他 host（含内网地址）不做任何改写。
     *
     * @param model 页面渲染 model
     */
    private void upgradeOssAssetsToHttps(Map<String, Object> model) {
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                entry.setValue(RichTextUtils.upgradeOssToHttps((String) value));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                upgradeOssAssetsToHttps(nestedMap);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof String) {
                        list.set(i, RichTextUtils.upgradeOssToHttps((String) item));
                    } else if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapInList = (Map<String, Object>) item;
                        upgradeOssAssetsToHttps(mapInList);
                    }
                }
            }
        }
    }

    /**
     * 渲染成功后收割通用上下文（渲染完成态快照，供搜索页合并）；
     * 白名单与浅拷贝规则见 SiteCommonContextCache。
     */
    private void harvestCommonContext(Map<String, Object> model, String host) {
        try {
            Object currentLang = model.get("currentLang");
            String lang = currentLang == null ? "en" : currentLang.toString();
            siteCommonContextCache.put(host, lang, model);
        } catch (Exception e) {
            // 收割失败不影响页面渲染
            log.warn("harvest common context failed, host:{}", host);
        }
    }

    /**
     * 站点内容列表接口签名：sign = sha256Hex("reqTime={reqTime}&accessKey={accessKey}&secretKey={secretKey}")
     */
    static String buildSiteContentSign(long reqTime, String accessKey, String secretKey) {
        String plain = "reqTime=" + reqTime + "&accessKey=" + accessKey + "&secretKey=" + secretKey;
        return SignUtils.sha256(plain);
    }

    /**
     * 组装站点内容搜索接口完整查询串（业务参数 + 鉴权参数）。
     * 接口口径（实测 192.168.35.16 联调环境，GET /api/siteContent/search）：
     * keyword 必传（缺失时接口 state=fail "keyword is required"），服务端按关键词过滤；
     * lang 为 mapLang 全码（en_US/fr_FR…）；分页 pageNum/pageSize（响应回显）；
     * scope 为类型范围（SearchScope.code，归一化后非 ALL 才下发，缺失/ALL 时接口按全类型过滤）；
     * 响应结构 data.pageData.{rows,totalPages} + data.association。
     *
     * @param host 当前请求归一化域名，用于取该域名对应的 site-content 配置（未配置时回退默认值）
     * @return 查询串；accessKey/secretKey 未配置时返回 null，由调用方走空结果兜底
     */
    String buildSiteContentQuery(String lang, String keyword, String channelCid, SearchScope searchScope, int pageNum, String host) {
        String accessKey = siteContentProperties.getAccessKeyByDomain(host);
        String secretKey = siteContentProperties.getSecretKeyByDomain(host);
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            log.warn("site-content accessKey/secretKey is not configured, host:{}, skip request", host);
            return null;
        }
        long reqTime = System.currentTimeMillis() / 1000L;
        String sign = buildSiteContentSign(reqTime, accessKey, secretKey);
        StringBuilder query = new StringBuilder();
        query.append("keyword=").append(urlEncode(StringUtils.defaultString(keyword)));
        String apiLang = resolveApiLang(lang);
        if (StringUtils.isNotBlank(apiLang)) {
            query.append("&lang=").append(urlEncode(apiLang));
        }
        if (StringUtils.isNotBlank(channelCid)) {
            query.append("&channelCid=").append(urlEncode(channelCid));
        }
        if (!SearchScope.isAll(searchScope)) {
            query.append("&scope=").append(urlEncode(searchScope.getCode()));
        }
        query.append("&pn=").append(Math.max(1, pageNum));
        if (siteContentProperties.getPageSize() != null) {
            query.append("&ps=").append(siteContentProperties.getPageSize());
        }
        query.append("&accessKey=").append(urlEncode(accessKey));
        query.append("&reqTime=").append(reqTime);
        query.append("&sign=").append(sign);
        return query.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
        } catch (Exception e) {
            return StringUtils.defaultString(value);
        }
    }

    /**
     * 将站点语言短码（en/fr/…）解析为接口语种代码（brand.mapLang 中的 en_US/fr_FR/…）。
     * 英语返回 en_US；无法匹配时返回原值（联调兜底）。
     */
    String resolveApiLang(String lang) {
        if (StringUtils.isBlank(lang)) {
            return "en_US";
        }
        List<String> mapLang = brandConfig == null ? null : brandConfig.getMapLang();
        if (mapLang != null) {
            for (String code : mapLang) {
                if (code.toLowerCase().startsWith(lang.toLowerCase() + "_")) {
                    return code;
                }
            }
        }
        return lang;
    }

    /**
     * 站内搜索
     * <p>
     * 调用方传入站点语言短码（en/de/fr…，与 URL 语种前缀一致）；
     * 上下文缓存与页面模型直接使用短码，搜索接口查询由
     * {@link #buildSiteContentQuery} 内的 {@link #resolveApiLang}
     * 基于 BrandConfig.mapLang 将短码解析为完整语种 code（de → de_DE）后发起。
     *
     * @param keyword 搜索关键词（原始值，模型中经 urlEncode 拼入分页/语种切换链接）
     * @param channelCid 分类 ID（分类搜索时透传，空为全站搜索）
     * @param scope 类型范围原始值（浏览器查询串，按 SearchScope.code 归一化，空/未知回退 ALL）
     * @param pageNum 页码（空或非法按 1 处理）
     * @param lang 站点语言短码（与 URL 语种前缀一致）
     * @param host 当前请求归一化域名
     */
    public DomainHtmlVo searchSiteContent(String keyword, String channelCid, String scope, String pageNum, String lang, String host) {
        Map<String, Object> model = new HashMap<>();
        SearchScope searchScope = resolveSearchScope(scope, host);
        fillCommonModel(model, host);
        int page = parsePageNum(pageNum);
        model.put("pageData", emptyPageData(page));
        // 通用上下文缓存 miss（该语种尚无页面渲染收割过）时，先经后端 /home?lang=xx 链路预取一次，
        // 其内部渲染成功后会收割 advanced 等语种级公共变量，避免搜索页渲染时 head.html 取 advanced 报空
        if (siteCommonContextCache.get(host, lang) == null) {
            try {
                getFromApi(restTemplate,
                        "/home" + (StringUtils.isBlank(lang) ? "" : "?lang=" + lang), host);
            } catch (Exception e) {
                log.warn("search page common context warmup failed, host:{}, lang:{}", host, lang, e);
            }
        }
        siteCommonContextCache.mergeInto(host, lang, model);
        fillSearchPageModel(model, lang, host);
        // 顶部导航优先取通用上下文缓存（后端渲染完成态，i18nName 为该槽位语种译文）；
        // 缓存未命中时回退本地菜单（getMenu 接口 i18nName 为空，补 pageName 兜底）
        if (model.get("topNavPages") == null) {
            List<SitePage> topNavPages = new ArrayList<>(SiteMenuUtils.getMenuPages(host));
            for (SitePage navItem : topNavPages) {
                if (navItem.getI18nName() == null) {
                    navItem.setI18nName(navItem.getPageName());
                }
            }
            model.put("topNavPages", topNavPages);
        }
        model.put("currentScope", searchScope.getCode());
        model.put("scopeTabs", buildScopeTabs(searchScope, host, model, keyword));
        String normKeyword = StringUtils.isNotBlank(keyword)
                ? ThymeleafUtils.normalizeString(keyword) : "";
        if (StringUtils.isNotBlank(normKeyword)) {
            model.put("selectLabel", StringUtils.isNotBlank(channelCid)
                    ? normKeyword + "-" + channelCid : normKeyword);
        }
        // 搜索页 URL 为查询参数形态 /{lang}/search?keyword=&scope=&page=：
        // searchKeywordParam（仅关键词）供类型 Tab 链接重组，
        // searchQueryBase（关键词+类型）供分页组件追加 page 参数与语种切换重构链接，
        // cid 为遗留参数仅在入口请求透传远程接口，页面生成的 URL 不再携带（过滤维度统一为 keyword+scope）
        model.put("pageName", "search");
        String searchKeywordParam = StringUtils.isNotBlank(keyword)
                ? "keyword=" + urlEncode(keyword) : "";
        StringBuilder queryBase = new StringBuilder(searchKeywordParam);
        if (!SearchScope.isAll(searchScope)) {
            if (queryBase.length() > 0) {
                queryBase.append("&");
            }
            queryBase.append("scope=").append(searchScope.getCode());
        }
        String searchQueryBase = queryBase.toString();
        model.put("searchKeywordParam", searchKeywordParam);
        model.put("searchQueryBase", searchQueryBase);
        model.put("suffix", StringUtils.isNotBlank(searchQueryBase) ? "?" + searchQueryBase : "");
        Map<String, Object> pageData = new HashMap<>();
        try {
            // 传入短码，resolveApiLang 基于 BrandConfig.mapLang 解析为完整语种 code（de → de_DE）后再查询
            String query = buildSiteContentQuery(lang, keyword, channelCid, searchScope, page, host);
            if (query == null) {
                return fallbackToBackendSearch(model, keyword, channelCid, page, lang, host);
            }
            String url = siteContentProperties.getBaseUrl() + "/api/siteContent/search?" + query;
            log.info("request siteContent list, host:{}, pageNum:{}, lang:{}", host, page, lang);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("siteContent list request failed, status:{}", response.getStatusCodeValue());
                return fallbackToBackendSearch(model, keyword, channelCid, page, lang, host);
            }
            JSONObject body = JSON.parseObject(response.getBody());
            if (body == null || "fail".equalsIgnoreCase(body.getString("state"))) {
                log.error("siteContent list request rejected, msg:{}", body == null ? "empty body" : body.getString("msg"));
                return fallbackToBackendSearch(model, keyword, channelCid, page, lang, host);
            }
            mapToPageData(body, pageData, page, host, model);
            model.put("pageData", pageData);

            model.put("selectValue",keyword);
            // association/category1/category2（分类侧栏数据）随侧栏移除不再进入模板
            mapIfPresent(dataObject(body), "noResults", model);
            mapIfPresent(dataObject(body), "keptQuery", model);
            mapIfPresent(dataObject(body), "searchNav", model);
            mapIfPresent(dataObject(body), "sourcingRequest", model);
        } catch (Exception e) {
            log.error("siteContent list request exception, pageNum:{}, lang:{}", page, lang, e);
            return fallbackToBackendSearch(model, keyword, channelCid, page, lang, host);
        }
        return renderSearchPage(model, host);
    }

    /**
     * siteContent 搜索接口不可用（鉴权未配置/请求异常/非 2xx/state=fail）时的兜底：
     * 回退后端 CMS 旧搜索接口取数——
     * 分类搜索（带 cid）走 /public/search（catePaging，必传 searchKey/categoryId/pageNum），
     * 全站搜索走 /public/url（publicUrl，menuName=search&docName=search，paging 以 id 收关键词；
     * docName 缺失时后端搜索分支会 NPE，必传）。
     * 后端返回模型是旧模板口径（行数据缺 detailUrlPrefix/slugUrl，模型缺 searchQueryBase），
     * 直接渲染会 SpEL 缺键报错，故只取其 pageData 行数据，补齐新模板必需字段后并入本地模型渲染。
     * 旧接口无 scope 维度，类型 Tab 移除（模板 th:if 隐藏）；
     * 旧链路也失败（空数据/异常）时同样移除 scopeTabs 后渲染本地空结果页。
     *
     * @param model 已组装的搜索页本地模型（含 searchQueryBase/currentPathLang 等渲染脚手架）
     * @param keyword 搜索关键词原始值
     * @param channelCid 分类 ID（非空走分类搜索旧链路，空走全站搜索旧链路）
     * @param page 页码（空或非法按 1 处理）
     * @param lang 站点语言短码（与 URL 语种前缀一致）
     * @param host 当前请求归一化域名
     */
    private DomainHtmlVo fallbackToBackendSearch(Map<String, Object> model, String keyword, String channelCid,
                                                 int page, String lang, String host) {
        StringBuilder path = new StringBuilder();
        if (StringUtils.isNotBlank(channelCid)) {
            path.append("/public/search?searchKey=").append(urlEncode(StringUtils.defaultString(keyword)))
                    .append("&categoryId=").append(urlEncode(channelCid))
                    .append("&pageNum=").append(Math.max(1, page));
        } else {
            path.append("/public/url?menuName=search&docName=search")
                    .append("&id=").append(urlEncode(StringUtils.defaultString(keyword)))
                    .append("&pageNum=").append(Math.max(1, page));
        }
        if (StringUtils.isNotBlank(lang)) {
            path.append("&lang=").append(urlEncode(lang));
        }
        try {
            Map<String, Object> pageData = fetchLegacySearchPageData(path.toString(), host, page, model);
            if (pageData != null) {
                model.put("pageData", pageData);
                model.put("selectValue", keyword);
                // 旧搜索接口不返回空结果文案（新接口随响应下发），固定英文兜底，避免模板渲染出 null
                model.putIfAbsent("noResults", "No results for");
                model.putIfAbsent("keptQuery", "We kept your original query. Try a related keyword, browse a broader "
                        + "category, remove filters or publish a sourcing request.");
                model.putIfAbsent("sourcingRequest", "Post a sourcing request");
                model.remove("scopeTabs");
                log.info("siteContent search unavailable, fallback to backend search succeeded, host:{}, lang:{}", host, lang);
                return renderSearchPage(model, host);
            }
            log.error("backend search fallback returned empty, host:{}, lang:{}", host, lang);
        } catch (Exception e) {
            log.error("backend search fallback exception, host:{}, lang:{}", host, lang, e);
        }
        model.remove("scopeTabs");
        return renderSearchPage(model, host);
    }

    /**
     * 请求后端旧搜索接口并转换为本地 pageData。后端行数据为原始 Prod 结构（id/name/url），
     * 缺新模板必需键（SpEL 对 Map 缺键会抛 EL1008E），渲染前补齐：
     * type 统一按 products（旧搜索接口仅检索产品）、slugUrl 缺失按 name 生成 smart slug、
     * description/enName 空值兜底，再经 {@link #decorateRows} 补 typeLabel 与 detailUrlPrefix。
     *
     * @param path 后端搜索接口路径（getFromApi 同源拼接口径，wcmApi + path）
     * @param host 当前请求归一化域名
     * @param page 页码（后端未回显时的兜底值）
     * @param model 页面模型（decorateRows 解析菜单级 typeLabel/detailUrlPrefix 使用）
     * @return pageData（rows/pageNum/totalPages）；接口失败、非 2xx 或 data 缺失时返回 null
     */
    private Map<String, Object> fetchLegacySearchPageData(String path, String host, int page, Map<String, Object> model) {
        String apiPath = path.startsWith("/") ? path.substring(1) : path;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deploy-Domain", host);
        ResponseEntity<String> response = restTemplate.exchange(wcmApi + apiPath, HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("legacy search request failed, path:{}, status:{}", apiPath, response.getStatusCodeValue());
            return null;
        }
        JSONObject body = JSON.parseObject(response.getBody());
        if (body == null || body.getInteger("code") == null || body.getInteger("code") != 200) {
            log.error("legacy search request rejected, path:{}, msg:{}",
                    apiPath, body == null ? "empty body" : body.getString("msg"));
            return null;
        }
        JSONObject data = body.getJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONObject pd = data.getJSONObject("pageData");
        JSONArray rows = pd == null ? null : pd.getJSONArray("rows");
        if (rows != null) {
            for (Object item : rows) {
                if (!(item instanceof JSONObject)) {
                    continue;
                }
                JSONObject row = (JSONObject) item;
                if (StringUtils.isBlank(row.getString("type"))) {
                    row.put("type", "prod");
                }
                if (StringUtils.isBlank(row.getString("slugUrl"))) {
                    row.put("slugUrl", ThymeleafUtils.resolveSlugUrl(null, row.getString("name")));
                }
                if (row.getString("description") == null) {
                    row.put("description", "");
                }
                if (!row.containsKey("enName")) {
                    row.put("enName", "");
                }
            }
        }
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("rows", decorateRows(rows, host, model));
        int pageNum = page;
        int totalPages = 0;
        if (pd != null) {
            try {
                pageNum = Integer.parseInt(pd.getString("pageNum"));
            } catch (NumberFormatException ignored) {
            }
            try {
                totalPages = Integer.parseInt(pd.getString("totalPages"));
            } catch (NumberFormatException ignored) {
            }
        }
        pageData.put("pageNum", pageNum);
        pageData.put("totalPages", totalPages);
        return pageData;
    }

    private JSONObject dataObject(JSONObject body) {
        Object data = body.get("data");
        return data instanceof JSONObject ? (JSONObject) data : null;
    }

    private void mapIfPresent(JSONObject dataObj, String key, Map<String, Object> model) {
        if (dataObj != null) {
            Object value = dataObj.get(key);
            if (value != null) {
                model.put(key, value);
            }
        }
    }

    /**
     * 归一化搜索 scope：空白按 ALL；未知 code（含已下线类型，如 formula）记 warn 日志后按 ALL 处理
     *
     * @param scope 浏览器查询串原始 scope 值
     * @param host 当前请求归一化域名
     * @return 归一化后的 scope 枚举，绝不返回 null
     */
    private SearchScope resolveSearchScope(String scope, String host) {
        String code = StringUtils.trimToEmpty(scope);
        SearchScope searchScope = SearchScope.fromCode(code);
        if (StringUtils.isNotBlank(code) && !SearchScope.allCodes().contains(code)) {
            log.warn("unknown search scope, fallback to all, host:{}, scope:{}", host, code);
        }
        return searchScope;
    }

    /**
     * 组装搜索结果页类型 Tab 模型（顺序即 SearchScope 常量声明顺序）。
     * label 经 {@link #resolveScopeLabel} 解析，href 携带当前关键词与 scope（ALL 省略 scope 参数）。
     *
     * @param current 当前归一化后的 scope
     * @param host 当前请求归一化域名
     * @param model 页面模型（读取 currentPathLang 拼 Tab 链接）
     * @param keyword 搜索关键词原始值（Tab 链接携带，保持筛选上下文）
     * @return Tab 模型列表，元素含 code/label/href/active
     */
    private List<Map<String, String>> buildScopeTabs(SearchScope current, String host, Map<String, Object> model, String keyword) {
        Object langPath = model.get("currentPathLang");
        String currentPathLang = langPath instanceof String ? (String) langPath : "";
        List<Map<String, String>> tabs = new ArrayList<>();
        for (SearchScope scope : SearchScope.values()) {
            Map<String, String> tab = new HashMap<>();
            tab.put("code", scope.getCode());
            tab.put("label", resolveScopeLabel(scope, host, model));
            tab.put("href", buildScopeTabHref(scope, currentPathLang, keyword));
            tab.put("active", scope == current ? "true" : "false");
            tabs.add(tab);
        }
        return tabs;
    }

    /**
     * 解析类型 Tab 显示名称：ALL 取 advanced 字典 all 键（与首页 tabs-new 同源）；
     * 其余 scope 经 pageType 映射匹配站点菜单页取 i18nName（与首页分类名称同源），
     * 菜单缺失或名称为空时回退英文默认文案，不渲染空白。
     *
     * @param scope scope 枚举
     * @param host 当前请求归一化域名
     * @param model 页面模型（读取 advanced 语种字典）
     * @return Tab 显示名称
     */
    private String resolveScopeLabel(SearchScope scope, String host, Map<String, Object> model) {
        if (SearchScope.isAll(scope)) {
            Object advanced = model.get("advanced");
            if (advanced instanceof Map) {
                Object all = ((Map<?, ?>) advanced).get("all");
                if (all instanceof String && StringUtils.isNotBlank((String) all)) {
                    return (String) all;
                }
            }
            return scope.getDefaultLabel();
        }
        return resolveMenuI18nLabel(scope, host, model, scope.getDefaultLabel());
    }

    /**
     * 按站点页面类型解析菜单 i18n 名称：scope 经 pageType 映射匹配菜单页取 i18nName
     * （与类型 Tab、导航菜单名称同源），匹配不到或名称为空时返回调用方给定的回退文案。
     * 数据源优先取 model 中合并自通用上下文缓存的 topNavPages（后端渲染完成态，
     * i18nName 为该槽位语种译文），缓存未命中或无译文时回退 SiteMenuUtils 本地菜单。
     *
     * @param scope scope 枚举
     * @param host 当前请求归一化域名
     * @param model 页面模型（读取合并后的 topNavPages）
     * @param fallback 菜单缺失或名称为空时的回退文案
     * @return 菜单页 i18n 名称或回退文案
     */
    private String resolveMenuI18nLabel(SearchScope scope, String host, Map<String, Object> model, String fallback) {
        if (scope.getPageType() != null) {
            for (SitePage menuPage : topNavPagesForScope(model, host)) {
                if (scope.getPageType().equals(menuPage.getPageType())
                        && StringUtils.isNotBlank(menuPage.getI18nName())) {
                    return menuPage.getI18nName();
                }
            }
        }
        return fallback;
    }

    /**
     * 获取用于语种名称解析的菜单页列表：优先取 model 中合并自通用上下文缓存的
     * topNavPages（元素为 Map 快照，按 pageType/i18nName/pageName 三字段还原），
     * 缓存未命中或结构异常时回退 SiteMenuUtils 本地菜单。
     *
     * @param model 页面模型（读取合并后的 topNavPages）
     * @param host 当前请求归一化域名（回退本地菜单时使用）
     * @return 当前语种菜单页列表，非空
     */
    private List<SitePage> topNavPagesForScope(Map<String, Object> model, String host) {
        Object cached = model.get("topNavPages");
        if (cached instanceof List && !((List<?>) cached).isEmpty()) {
            Object first = ((List<?>) cached).get(0);
            if (!(first instanceof SitePage)) {
                List<SitePage> restored = new ArrayList<>();
                for (Object item : (List<?>) cached) {
                    if (!(item instanceof Map)) {
                        return SiteMenuUtils.getMenuPages(host);
                    }
                    Map<?, ?> entry = (Map<?, ?>) item;
                    SitePage page = new SitePage();
                    Object pageType = entry.get("pageType");
                    if (pageType instanceof Number) {
                        page.setPageType(((Number) pageType).longValue());
                    }
                    Object i18nName = entry.get("i18nName");
                    page.setI18nName(i18nName instanceof String ? (String) i18nName : null);
                    Object pageName = entry.get("pageName");
                    page.setPageName(pageName instanceof String ? (String) pageName : null);
                    restored.add(page);
                }
                return restored;
            }
            return (List<SitePage>) cached;
        }
        return SiteMenuUtils.getMenuPages(host);
    }

    /**
     * 组装类型 Tab 跳转链接：{currentPathLang}/search?keyword=…&scope=…（页码固定第 1 页，scope 为 ALL 时省略）
     *
     * @param scope Tab 对应 scope
     * @param currentPathLang 语种路径前缀（英语为空串）
     * @param keyword 搜索关键词原始值
     * @return Tab 跳转链接
     */
    private String buildScopeTabHref(SearchScope scope, String currentPathLang, String keyword) {
        StringBuilder href = new StringBuilder(StringUtils.defaultString(currentPathLang)).append("/search");
        List<String> params = new ArrayList<>();
        if (StringUtils.isNotBlank(keyword)) {
            params.add("keyword=" + urlEncode(keyword));
        }
        if (!SearchScope.isAll(scope)) {
            params.add("scope=" + scope.getCode());
        }
        if (!params.isEmpty()) {
            href.append('?').append(String.join("&", params));
        }
        return href.toString();
    }

    /**
     * 补齐搜索页模板依赖的页面级公共变量
     */
    private void fillSearchPageModel(Map<String, Object> model, String lang, String host) {
        String currentLang = StringUtils.defaultIfBlank(lang, "en");
        model.putIfAbsent("currentLang", currentLang);
        model.putIfAbsent("currentPathLang", "en".equals(currentLang) ? "" : "/" + currentLang);
        model.putIfAbsent("langName", "en".equals(currentLang) ? "English" : currentLang);
        model.putIfAbsent("langEName", "en".equals(currentLang) ? "english" : currentLang);
        model.putIfAbsent("pageName", "search");
        model.putIfAbsent("suffix", "");
        model.putIfAbsent("websitePath", "https://" + host + "/");
        model.putIfAbsent("fontColor", "#FF5656");
        model.putIfAbsent("headColor", "#FFFFFF");
        model.putIfAbsent("headFontColor", "#000000");
        model.putIfAbsent("otherHeadFontColor", "#000000");
        Object langList = model.get("langList");
        if (langList == null || ((List<?>) langList).isEmpty()) {
            List<Map<String, String>> fallback = new ArrayList<>();
            for (String code : SiteMenuUtils.getLangList(host)) {
                Map<String, String> item = new HashMap<>();
                item.put("lang", code);
                item.put("langName", code);
                fallback.add(item);
            }
            model.put("langList", fallback);
        }
    }

    /**
     * 响应到 pageData 的单点映射。
     * 实测 /api/siteContent/search 结构：data.pageData.rows（行数据）、
     * data.pageData.totalPages（总页数）、data.association（分类侧边栏，可空数组）。
     * 行字段实测：{id, name, url, type, description, slugUrl, enName}——type 为
     * SearchScope.code，经 {@link #decorateRowType} 就地补充 typeLabel 展示字段。
     * 联调字段名变化时在此校准。
     *
     * @param body 远程接口响应体
     * @param pageData 页面模型 pageData 容器
     * @param page 当前页码（响应缺失页码字段时兜底）
     * @param host 当前请求归一化域名（解析菜单页用于链接前缀）
     * @param model 页面模型（读取合并后的 topNavPages，供 typeLabel/详情前缀按语种解析）
     */
    private void mapToPageData(JSONObject body, Map<String, Object> pageData, int page, String host, Map<String, Object> model) {
        JSONObject dataObj = dataObject(body);
        if (!(dataObj != null && dataObj.get("pageData") instanceof JSONObject)) {
            pageData.put("rows", new ArrayList<>());
            pageData.put("pageNum", page);
            pageData.put("totalPages", 0);
            return;
        }
        JSONObject pd = (JSONObject) dataObj.get("pageData");
        Object rows = pd.get("rows");
        pageData.put("rows", decorateRows(rows, host, model));
        Object pageNum = pd.get("pageNum");
        try {
            pageData.put("pageNum", pageNum == null ? page : Integer.parseInt(pageNum.toString()));
        } catch (NumberFormatException e) {
            pageData.put("pageNum", page);
        }
        Object totalPages = pd.get("totalPages");
        try {
            pageData.put("totalPages", totalPages == null ? 0 : Integer.parseInt(totalPages.toString()));
        } catch (NumberFormatException e) {
            pageData.put("totalPages", 0);
        }
    }

    /**
     * 为行数据补充展示与跳转字段：
     * <ul>
     * <li>typeLabel：行 type（SearchScope.code）经 {@link SearchScope#fromCode} 归一化后取
     * 英文默认文案（如 Products/Products/Documents 口径由模板大写渲染）；type 缺失或未知时置空</li>
     * <li>detailUrlPrefix：按行 type 对应的站点页面类型从菜单页解析详情链接前缀——
     * 产品/资料为 {菜单页pageName}/detail/，解决方案/成功案例/blog 为 {菜单页pageName}/；
     * 菜单缺失时回退 ''，由模板侧兜底</li>
     * </ul>
     *
     * @param rows 远程接口返回的行数据数组（可为 null）
     * @param host 当前请求归一化域名
     * @return 补充字段后的行数据列表；入参非数组时返回空列表
     */
    private List<Object> decorateRows(Object rows, String host, Map<String, Object> model) {
        List<Object> decorated = new ArrayList<>();
        if (!(rows instanceof JSONArray)) {
            return decorated;
        }
        for (Object item : (JSONArray) rows) {
            if (item instanceof JSONObject) {
                JSONObject row = (JSONObject) item;
                SearchScope scope = SearchScope.fromCode(row.getString("type"));
                // typeLabel 与类型 Tab 同源：该语种菜单页 i18nName（缓存 topNavPages 优先），缺失回退英文文案
                row.put("typeLabel", SearchScope.isAll(scope)
                        ? "" : resolveMenuI18nLabel(scope, host, model, scope.getDefaultLabel()));
                row.put("detailUrlPrefix", resolveDetailUrlPrefix(scope, host, model));
            }
            decorated.add(item);
        }
        return decorated;
    }

    /**
     * 解析行数据的详情链接前缀：scope 经 pageType 映射匹配菜单页取 pageName
     * （空格折叠为连字符，与列表模板链接口径一致）。产品(PRODUCTS)与资料(DOCUMENTS)
     * 前缀含 /detail/ 段，解决方案/成功案例/blog 直接为菜单页路径段。
     * 菜单页数据源与语种名称解析同源（缓存 topNavPages 优先，回退本地菜单）。
     *
     * @param scope 行内容类型
     * @param host 当前请求归一化域名（回退本地菜单时使用）
     * @param model 页面模型（读取合并后的 topNavPages）
     * @return 形如 /{pageName}/ 或 /{pageName}/detail/ 的前缀；菜单页缺失时返回 /
     */
    private String resolveDetailUrlPrefix(SearchScope scope, String host, Map<String, Object> model) {
        if (scope.getPageType() != null) {
            for (SitePage menuPage : topNavPagesForScope(model, host)) {
                if (scope.getPageType().equals(menuPage.getPageType())
                        && StringUtils.isNotBlank(menuPage.getPageName())) {
                    String seg = "/" + StringUtils.replace(StringUtils.trim(menuPage.getPageName()), " ", "-") + "/";
                    boolean withDetail = SearchScope.PROD == scope || SearchScope.DOCUMENT == scope;
                    return withDetail ? seg + "detail/" : seg;
                }
            }
        }
        return "/";
    }

    private Map<String, Object> emptyPageData(int page) {
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("rows", new ArrayList<>());
        pageData.put("pageNum", page);
        pageData.put("totalPages", 0);
        return pageData;
    }

    private DomainHtmlVo renderSearchPage(Map<String, Object> model, String host) {
        DomainHtmlVo domainHtmlVo = new DomainHtmlVo();
        String content = templateEnginePageRenderer.renderPage("search-details", model);
        domainHtmlVo.setHtmlContent(content);
        domainHtmlVo.setModelMap(model);
        return domainHtmlVo;
    }

    /**
     * 按语种预热搜索页通用上下文缓存（启动时后台调用）：
     * 逐语种经后端 /home?lang=xx 链路渲染一次 home，触发 SiteCommonContextCache 收割，
     * 避免冷启动后首个非英语搜索触发同步预热造成首查延迟。
     * 已缓存的语种跳过；单语种失败仅告警不阻断。
     */
    public void warmupCommonContext(String domain) {
        try {
            List<String> langs = SiteMenuUtils.getLangList(domain);
            for (String lang : langs) {
                if (siteCommonContextCache.get(domain, lang) != null) {
                    continue;
                }
                try {
                    getFromApi(restTemplate,
                            "/home" + (StringUtils.isBlank(lang) ? "" : "?lang=" + lang), domain);
                } catch (Exception e) {
                    log.warn("warmup common context failed, domain:{}, lang:{}", domain, lang, e);
                }
            }
            log.info("warmup common context done, domain:{}, langs:{}", domain, langs.size());
        } catch (Exception e) {
            log.warn("warmup common context error, domain:{}", domain, e);
        }
    }

    private int parsePageNum(String pageNum) {
        try {
            return Integer.parseInt(pageNum);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 填充各页面渲染共用的公共 model 变量（CDN、语言、统计、宿主信息等）
     */
    private void fillCommonModel(Map<String, Object> model, String host) {
        model.put("cdnEnabled", cdnEnabled);
        model.put("cdnPrefix", cdnPrefix);
        model.put("landingBaseUrl", landingBaseUrl);
        model.put("version", version);
        if (model.containsKey("brandUrl")) {
            String brandUrl = model.get("brandUrl").toString();
            model.put("brandUrl", brandUrl.toLowerCase());
        }
        if (model.containsKey("currentLang")
                && model.get("currentLang") != null && !model.get("currentLang")
                .equals("en")) {
            model.put("langEName", languageMap.get(model.get("currentLang").toString()).toLowerCase());
        } else {
            model.put("langEName", "english");
        }
        model.put("currentHost", host);
        String tagId = googleTagProperties.getId();
        if (googleTagProperties.getDomains().containsKey(host)) {
            tagId = googleTagProperties.getDomains().get(host);
        }
        model.put("year", Calendar.getInstance().get(Calendar.YEAR));
        model.put("gaTrackingId", tagId);
    }

    private void handlePlatformSuccessCaseLink(Map<String, Object> model) {
        Object currentLang = model.get("currentLang");
        if (currentLang == null || currentLang.toString().startsWith("en")){
            return;
        }
        if (brandConfig.getCaseTranslateOnline()){
            return;
        }
        Object caseList = model.get("relatedSuccessCases");
        if (caseList instanceof List ){
            List<Object> list = (List<Object>) caseList;
            for (Object item : list){
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapInList = (Map<String, Object>) item;
                    Object cardUrl = mapInList.get("cardUrl");
                    if (cardUrl != null && cardUrl.toString().contains("www.iee-business.com")){
                        List<String> mapLang = brandConfig.getMapLang();
                        for (String lang: mapLang){
                            if (lang.startsWith(currentLang.toString())){
                                String newCardUrl = cardUrl.toString().replace("-"+lang,"");
                                mapInList.put("cardUrl",newCardUrl);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleSchemaJson(Map<String, Object> model, String websiteUrl, String host) {
        for (String key : model.keySet()) {
            if (key.toLowerCase().endsWith("schemajson") &&
                    (model.get(key) instanceof Map || model.get(key) instanceof List)) {
                String newVal = JSON.toJSONString(model.get(key));
                if (newVal.startsWith("{")) {
                    JSONObject object = JSON.parseObject(newVal);
                    if (StringUtils.isNotEmpty(websiteUrl)) {
                        if (object.containsKey("offers")) {
                            //修改产品url
                            object.getJSONObject("offers").put("url", websiteUrl);
                        } else if (object.get("@type").toString().toLowerCase().contains("article")) {
                            //修改文章、解决方案url
                            JSONObject mainEntityOfPage = object.getJSONObject("mainEntityOfPage");
                            if (mainEntityOfPage != null) {
                                mainEntityOfPage.put("@id", websiteUrl);
                            }
                        }
                    }
                    newVal = object.toJSONString();
                } else if (newVal.startsWith("[") && model.containsKey("templateName") && model.get("templateName").equals("index")) {
                    //修改首页website类型添加name
                    JSONArray array = JSON.parseArray(newVal);
                    for (int i = 0; i < array.size(); i++) {
                        if (array.getJSONObject(i).containsKey("@type")
                                && array.getJSONObject(i).getString("@type").equals("WebSite")
                                && !array.getJSONObject(i).containsKey("name")) {
                            array.getJSONObject(i).put("name", host);
                        }
                    }
                    newVal = array.toJSONString();
                }
                model.put(key, newVal);
            } else if (key.toLowerCase().contains("breadcrumb") && model.get(key) instanceof String) {
                JSONObject object = JSON.parseObject(model.get(key).toString());
                JSONArray itemListElement = object.getJSONArray("itemListElement");
                List<String> mapLang = brandConfig.getMapLang();
                for (int i = 0; i < itemListElement.size(); i++) {
                    String itemUrl = itemListElement.getJSONObject(i).getString("item");
                    for (String mLang : mapLang) {
                        if (itemUrl.contains(mLang)) {
                            if (mLang.equals("en_US")){
                                itemUrl = itemUrl.replace("en_US/", "");
                            }else{
                                itemUrl = itemUrl.replace(mLang, mLang.split("_")[0]);
                            }
                            itemListElement.getJSONObject(i).put("item", itemUrl);
                            break;
                        }
                    }
                }
                model.put(key, object.toJSONString());
            }
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss+08:00");
    private static final DateTimeFormatter REQUEST_LOG_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter REQUEST_LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    List<String> dateList = Arrays.asList("created", "updated");

    private void handleDateKey(Map<String, Object> model) {
        handleDateKey(model, null);
    }

    private void handleDateKey(Map<String, Object> model, String parentKey) {
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // advanced.updated 是页面文案，不是日期字段。
            if ("advanced".equals(parentKey) && "updated".equals(key)) {
                continue;
            }
            // 1. 如果当前值是需要转换的日期键，且是字符串类型，则进行转换
            if (dateList.contains(key) && value instanceof String
                    && !ObjectUtils.isEmpty(value)) {
                try {
                    String date = (String) value;
                    if (date.length() > 10) {
                        date = date.substring(0, 10);
                    }
                    LocalDate parsedDate = LocalDate.parse(date, date.length() == 10 ? DATE_FORMATTER : TIME_FORMATTER);
                    model.put(key, Date.from(parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                } catch (Exception e) {
                    log.error("日期格式解析错误，键: {}, 值: {}", key, value);
                }
            }
            // 2. 如果当前值是一个嵌套的Map，则递归调用
            else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                handleDateKey(nestedMap, key);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapInList = (Map<String, Object>) item;
                        handleDateKey(mapInList, key);
                    }
                }
            }
        }
    }

    private void handleLibraryFileSize(Map<String, Object> model) {
        Object libraryValue = model.get("library");
        if (!(libraryValue instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> library = (Map<String, Object>) libraryValue;
        if ("2".equals(String.valueOf(library.get("type")))) {
            model.put("libraryFileSize", "");
            return;
        }
        Object fileValue = model.get("libraryFile");
        if (!(fileValue instanceof Map)) {
            model.put("libraryFileSize", "");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> libraryFile = (Map<String, Object>) fileValue;
        model.put("libraryFileSize", formatFileSize(libraryFile.get("size")));
    }

    private String formatFileSize(Object rawSize) {
        if (rawSize == null || StringUtils.isBlank(rawSize.toString())) {
            return "";
        }
        try {
            long bytes = Long.parseLong(rawSize.toString().trim());
            if (bytes < 0) {
                return "";
            }
            if (bytes < 1024) {
                return bytes + " B";
            }
            String[] units = {"KB", "MB", "GB", "TB"};
            double value = bytes;
            int unitIndex = -1;
            do {
                value /= 1024D;
                unitIndex++;
            } while (value >= 1024D && unitIndex < units.length - 1);
            String formattedValue = String.format(Locale.ROOT, "%.1f", value);
            if (formattedValue.endsWith(".0")) {
                formattedValue = formattedValue.substring(0, formattedValue.length() - 2);
            }
            return formattedValue + " " + units[unitIndex];
        } catch (NumberFormatException e) {
            return "";
        }
    }


    @SneakyThrows
    public ResponseEntity<String> forwardFormRequest(HttpServletRequest originalRequest, String targetUrl) {
        List<File> tempFiles = new ArrayList<>();
        boolean isMultipart = originalRequest.getContentType() != null
                && originalRequest.getContentType().startsWith("multipart/form-data");
        HttpEntity<?> requestEntity = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String deployDomain = originalRequest.getHeader("Host");
        headers.add("Deploy-Domain", deployDomain);
        headers.add("Premium-Real-IP", originalRequest.getHeader("X-Real-IP"));
        headers.add("Referer", originalRequest.getHeader("Referer"));
        String requestId = buildRequestId();
        Path requestLogDir = prepareRequestLogDir(deployDomain, requestId, targetUrl.contains("leaveMessage"));
        Map<String, Object> requestLog = buildBaseRequestLog(originalRequest, targetUrl, deployDomain, requestId, isMultipart);
        try {
            if (isMultipart) {
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                try {
                    MultipartHttpServletRequest multipartRequest =
                            (MultipartHttpServletRequest) originalRequest;
                    for (String paramName : multipartRequest.getFileMap().keySet()) {
                        MultipartFile file = multipartRequest.getFile(paramName);
                        if (file != null && !file.isEmpty()) {
                            File tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
                            try {
                                file.transferTo(tempFile);
                                tempFiles.add(tempFile);
                                FileSystemResource resource = new FileSystemResource(tempFile);
                                body.add(paramName, resource);
                            } catch (IOException e) {
                                throw new RuntimeException("文件传输失败: " + e.getMessage(), e);
                            }
                        }
                    }

                    for (String paramName : multipartRequest.getParameterMap().keySet()) {
                        String[] values = multipartRequest.getParameterValues(paramName);
                        for (String value : values) {
                            body.add(paramName, value);
                        }
                    }

                } catch (Exception e) {
                    requestLog.put("status", "parse_failed");
                    requestLog.put("error", e.getMessage());
                    persistRequestLog(requestLogDir, requestLog);
                    return ResponseEntity.badRequest().body("文件解析失败: " + e.getMessage());
                }
                requestEntity = new HttpEntity<>(body, headers);
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            } else {
                StringBuilder formBody = new StringBuilder();
                Map<String, String[]> parameterMap = new LinkedHashMap<>();
                Enumeration<String> parameterNames = originalRequest.getParameterNames();
                while (parameterNames.hasMoreElements()) {
                    String paramName = parameterNames.nextElement();
                    String[] paramValues = originalRequest.getParameterValues(paramName);
                    parameterMap.put(paramName, paramValues);
                    for (String value : paramValues) {
                        if (formBody.length() > 0) {
                            formBody.append("&");
                        }
                        String newV = value;
                        if (paramName.equals("name") &&
                                value.length() > 32) {
                            newV = value.substring(0, 32);
                        }
                        formBody.append(URLEncoder.encode(paramName, "UTF-8"))
                                .append("=")
                                .append(URLEncoder.encode(newV, "UTF-8"));
                    }
                }
                requestLog.put("formParams", parameterMap);
                requestLog.put("files", Collections.emptyList());
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                requestEntity = new HttpEntity<>(formBody.toString(), headers);
            }
            if (targetUrl.contains("leaveMessage")) {
                String email = originalRequest.getParameter("email");
                String phone = originalRequest.getParameter("phone");
                if (StringUtils.isBlank(email) || StringUtils.isBlank(phone)) {
                    requestLog.put("status", "validation_failed");
                    requestLog.put("error", "email and phone must not be empty");
                    persistRequestLog(requestLogDir, requestLog);
                    return ResponseEntity.badRequest().body("email and phone must not be empty");
                }
            }

            requestLog.put("status", "forwarding");
            if (targetUrl.contains("leaveMessage")) {
                persistRequestLog(requestLogDir, requestLog);
            }

            String url = "";
            if (targetUrl.equals("/upload")) {
                url = wcmApi.replace("static/", "") + targetUrl;
            } else {
                url = wcmApi + targetUrl;
            }
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            requestLog.put("status", "forwarded");
            requestLog.put("responseStatus", response.getStatusCodeValue());
            if (targetUrl.contains("leaveMessage")) {
                persistRequestLog(requestLogDir, requestLog);
            }
            log.info("forward request logged, requestId:{}, domain:{}, targetUrl:{}, dir:{}",
                    requestId, deployDomain, targetUrl, requestLogDir.toAbsolutePath());
            return response;
        } catch (Exception e) {
            requestLog.put("status", "forward_failed");
            requestLog.put("error", e.getMessage());
            persistRequestLog(requestLogDir, requestLog);
            throw e;
        } finally {
            for (File tempFile : tempFiles) {
                if (tempFile.exists()) {
                    FileUtil.clean(tempFile);
                }
            }
        }
    }

    private Map<String, Object> buildBaseRequestLog(HttpServletRequest originalRequest,
                                                    String targetUrl,
                                                    String deployDomain,
                                                    String requestId,
                                                    boolean isMultipart) {
        Map<String, Object> requestLog = new LinkedHashMap<>();
        requestLog.put("requestId", requestId);
        requestLog.put("loggedAt", REQUEST_LOG_TIME_FORMATTER.format(LocalDateTime.now()));
        requestLog.put("deployDomain", deployDomain);
        requestLog.put("targetUrl", targetUrl);
        requestLog.put("method", originalRequest.getMethod());
        requestLog.put("contentType", originalRequest.getContentType());
        requestLog.put("multipart", isMultipart);
        requestLog.put("requestUri", originalRequest.getRequestURI());
        requestLog.put("queryString", originalRequest.getQueryString());
        requestLog.put("realIp", originalRequest.getHeader("X-Real-IP"));
        requestLog.put("remoteAddr", originalRequest.getRemoteAddr());
        return requestLog;
    }


    private Path prepareRequestLogDir(String deployDomain, String requestId, boolean isRfq) throws IOException {
        String requestLogBaseDir = StringUtils.defaultIfBlank(brandConfig.getRequestLogDir(), "./request-logs");
        String safeDomain = sanitizeLogFileName(StringUtils.defaultIfBlank(deployDomain, "unknown-domain"));
        Path requestLogDir = Paths.get(requestLogBaseDir, safeDomain,
                LocalDate.now().format(REQUEST_LOG_DIR_FORMATTER), isRfq ? "leaveMsg" : "uploads", requestId);
        Files.createDirectories(requestLogDir);
        return requestLogDir;
    }

    private void persistRequestLog(Path requestLogDir, Map<String, Object> requestLog) {
        try {
            Files.write(requestLogDir.resolve("request.json"),
                    JSON.toJSONString(requestLog, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat).getBytes("UTF-8"));
        } catch (IOException e) {
            log.error("persist request log failed, dir:{}", requestLogDir, e);
        }
    }

    private String buildRequestId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String sanitizeLogFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "unknown";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }

    /**
     * 主动发起表单请求
     *
     * @param targetUrl 请求path
     * @param domain    域名
     * @param params    表单参数
     * @return
     */
    @SneakyThrows
    public ResponseEntity<String> submitForm(String targetUrl, String domain, Map<String, String> params) {
        StringBuilder formBody = new StringBuilder();
        for (String key : params.keySet()) {
            if (formBody.length() > 0) {
                formBody.append("&");
            }
            formBody.append(URLEncoder.encode(key, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(params.get(key), "UTF-8"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Deploy-Domain", domain);
        HttpEntity<String> requestEntity = new HttpEntity<>(formBody.toString(), headers);
        return callWithWcmApiPermit(() -> restTemplate.exchange(wcmApi + targetUrl, HttpMethod.POST, requestEntity, String.class));
    }

    /**
     * 首页
     *
     * @return 返回首页html
     */
    public DomainHtmlVo getHome(RestTemplate restTemplate, String host) {
        log.info("request Home data");
        try {
            return getFromApi(restTemplate, "home", host);
        } catch (Exception e) {
            log.error("request Home data exception", e);
            return null;
        }
    }

    /**
     * 主动发布前，查询当天更新的详情页ID（按页面类型分组）。
     */
    public Map<Integer, Set<Long>> getTodayUpdatedIds(String domain) {
        Map<Integer, Set<Long>> resultMap = new HashMap<>();
        try {
            ParameterizedTypeReference<AjaxResult<JSONObject>> typeReference =
                    new ParameterizedTypeReference<AjaxResult<JSONObject>>() {
                    };
            HttpHeaders headers = new HttpHeaders();
            headers.add("Deploy-Domain", domain);
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<AjaxResult<JSONObject>> response = callWithWcmApiPermit(() -> restTemplate.exchange(
                    wcmApi + "publish/todayUpdatedIds",
                    HttpMethod.GET,
                    requestEntity,
                    typeReference
            ));
            AjaxResult<JSONObject> ajaxResult = response.getBody();
            if (ajaxResult == null || ajaxResult.getCode() != 200 || ajaxResult.getData() == null) {
                log.warn("request todayUpdatedIds failed, domain:{}, resp:{}", domain, ajaxResult);
                return resultMap;
            }
            JSONObject data = ajaxResult.getData();
            resultMap.put(SitePage.SitePageType.PRODUCTS, parseIdSet(data,"prodIds"));
            resultMap.put(SitePage.SitePageType.SOLUTIONS, parseIdSet(data,"articleIds"));
            resultMap.put(SitePage.SitePageType.NEWS, parseIdSet(data,"newsIds"));
            resultMap.put(SitePage.SitePageType.SUCCESS_REFERENCE, parseIdSet(data, "caseIds"));
            log.info("todayUpdatedIds loaded, domain:{}, products:{}, solutions:{}, news:{}, success:{}",
                    domain,
                    resultMap.get(SitePage.SitePageType.PRODUCTS).size(),
                    resultMap.get(SitePage.SitePageType.SOLUTIONS).size(),
                    resultMap.get(SitePage.SitePageType.NEWS).size(),
                    resultMap.get(SitePage.SitePageType.SUCCESS_REFERENCE).size());
            return resultMap;
        } catch (Exception e) {
            log.error("request todayUpdatedIds exception, domain:{}", domain, e);
            return resultMap;
        }
    }

    private Set<Long> parseIdSet(JSONObject data, String key) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (StringUtils.isBlank(key) || !data.containsKey(key)) {
            return ids;
        }
        Object value = data.get(key);
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                appendLongId(ids, item);
            }
        } else if (value != null && value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            for (Object item : arr) {
                appendLongId(ids, item);
            }
        } else if (value instanceof String) {
            return Arrays.stream(value.toString().split(","))
                    .filter(org.springframework.util.StringUtils::hasText)
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
        }
        return ids;
    }

    private void appendLongId(Set<Long> ids, Object value) {
        if (value == null) {
            return;
        }
        try {
            String str = value.toString().trim();
            if (str.isEmpty()) {
                return;
            }
            ids.add(Long.parseLong(str));
        } catch (Exception ignore) {
            // ignore invalid id value
        }
    }
}