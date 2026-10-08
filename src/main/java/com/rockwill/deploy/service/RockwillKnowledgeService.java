package com.rockwill.deploy.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.conf.GoogleTagProperties;
import com.rockwill.deploy.conf.SiteContentProperties;
import com.rockwill.deploy.render.TemplateEnginePageRenderer;
import com.rockwill.deploy.utils.RichTextUtils;
import com.rockwill.deploy.utils.SignUtils;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.utils.ThymeleafUtils;
import com.rockwill.deploy.vo.AjaxResult;
import com.rockwill.deploy.vo.DomainHtmlVo;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
            ResponseEntity<AjaxResult<JSONObject>> response = restTemplate.exchange(
                    wcmApi + "getMenu",
                    HttpMethod.GET,
                    requestEntity,
                    typeReference
            );
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
        log.info("request wcm  {} ", path);
        try {
            ParameterizedTypeReference<AjaxResult<Map<String, Object>>> typeReference =
                    new ParameterizedTypeReference<AjaxResult<Map<String, Object>>>() {
                    };
            HttpHeaders headers = new HttpHeaders();
            headers.add("Deploy-Domain", host);
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<AjaxResult<Map<String, Object>>> responseEntity = restTemplate.exchange(
                    wcmApi + path,
                    HttpMethod.GET,
                    requestEntity,
                    typeReference
            );
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
                            String content = templateEnginePageRenderer.renderPage(templateName, model);
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
     * 无 type 参数；响应结构 data.pageData.{rows,totalPages} + data.association。
     *
     * @return 查询串；accessKey/secretKey 未配置时返回 null，由调用方走空结果兜底
     */
    String buildSiteContentQuery(String lang, String keyword, String channelCid, int pageNum) {
        if (StringUtils.isBlank(siteContentProperties.getAccessKey())
                || StringUtils.isBlank(siteContentProperties.getSecretKey())) {
            log.warn("site-content accessKey/secretKey is not configured, skip request");
            return null;
        }
        long reqTime = System.currentTimeMillis() / 1000L;
        String sign = buildSiteContentSign(reqTime, siteContentProperties.getAccessKey(), siteContentProperties.getSecretKey());
        StringBuilder query = new StringBuilder();
        query.append("keyword=").append(urlEncode(StringUtils.defaultString(keyword)));
        String apiLang = resolveApiLang(lang);
        if (StringUtils.isNotBlank(apiLang)) {
            query.append("&lang=").append(urlEncode(apiLang));
        }
        if (StringUtils.isNotBlank(channelCid)) {
            query.append("&channelCid=").append(urlEncode(channelCid));
        }
        query.append("&pageNum=").append(Math.max(1, pageNum));
        if (siteContentProperties.getPageSize() != null) {
            query.append("&pageSize=").append(siteContentProperties.getPageSize());
        }
        query.append("&accessKey=").append(urlEncode(siteContentProperties.getAccessKey()));
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
     */
    public DomainHtmlVo searchSiteContent(String keyword, String channelCid, String pageNum, String lang, String host) {
        Map<String, Object> model = new HashMap<>();
        fillCommonModel(model, host);
        int page = parsePageNum(pageNum);
        model.put("pageData", emptyPageData(page));
        siteCommonContextCache.mergeInto(host, lang, model);
        fillSearchPageModel(model, lang, host);
        List<SitePage> topNavPages = new ArrayList<>(SiteMenuUtils.getMenuPages(host));
        for (SitePage navItem : topNavPages) {
            if (navItem.getI18nName() == null) {
                navItem.setI18nName(navItem.getPageName());
            }
        }
        model.put("topNavPages", topNavPages);
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(channelCid)) {
                model.put("selectLabel", ThymeleafUtils.normalizeString(keyword) + "-" + channelCid);
            } else {
                model.put("selectLabel", ThymeleafUtils.normalizeString(keyword));
            }
        }
        Map<String, Object> pageData = new HashMap<>();
        try {
            String query = buildSiteContentQuery(lang, keyword, channelCid, page);
            if (query == null) {
                return renderSearchPage(model, host);
            }
            String url = siteContentProperties.getBaseUrl() + "/api/siteContent/search?" + query;
            log.info("request siteContent list, host:{}, pageNum:{}, lang:{}", host, page, lang);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("siteContent list request failed, status:{}", response.getStatusCodeValue());
                return renderSearchPage(model, host);
            }
            JSONObject body = JSON.parseObject(response.getBody());
            if (body == null || "fail".equalsIgnoreCase(body.getString("state"))) {
                log.error("siteContent list request rejected, msg:{}", body == null ? "empty body" : body.getString("msg"));
                return renderSearchPage(model, host);
            }
            mapToPageData(body, pageData, page);
            model.put("pageData", pageData);

            model.put("selectValue",keyword);
            mapIfPresent(dataObject(body), "association", model);
            mapIfPresent(dataObject(body), "category1", model);
            mapIfPresent(dataObject(body), "category2", model);
        } catch (Exception e) {
            log.error("siteContent list request exception, pageNum:{}, lang:{}", page, lang, e);
        }
        return renderSearchPage(model, host);
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
     * 行字段实测：{id, name, url}——与 search-details 模板的 index.name/index.url/index.id 直接对齐。
     * 联调字段名变化时在此校准。
     */
    private void mapToPageData(JSONObject body, Map<String, Object> pageData, int page) {
        JSONObject dataObj = dataObject(body);
        Object pageDataObj = dataObj == null ? null : dataObj.get("pageData");
        if (!(pageDataObj instanceof JSONObject)) {
            pageData.put("rows", new ArrayList<>());
            pageData.put("pageNum", page);
            pageData.put("totalPages", 0);
            return;
        }
        JSONObject pd = (JSONObject) pageDataObj;
        Object rows = pd.get("rows");
        pageData.put("rows", rows == null ? new ArrayList<>() : rows);
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
        return restTemplate.exchange(wcmApi + targetUrl, HttpMethod.POST, requestEntity, String.class);
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
            ResponseEntity<AjaxResult<JSONObject>> response = restTemplate.exchange(
                    wcmApi + "publish/todayUpdatedIds",
                    HttpMethod.GET,
                    requestEntity,
                    typeReference
            );
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