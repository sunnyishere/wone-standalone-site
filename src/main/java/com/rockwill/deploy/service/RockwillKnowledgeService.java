package com.rockwill.deploy.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.render.TemplateEnginePageRenderer;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.vo.AjaxResult;
import com.rockwill.deploy.vo.DomainHtmlVo;
import com.rockwill.deploy.vo.SitePage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Resource
    TemplateEnginePageRenderer templateEnginePageRenderer;

    private String wcmApi = "https://www.iee-business.com/wcm-api/site/static/";

    @PostConstruct
    public void initUrl() {
        if (devMode.equals("dev")) {
            wcmApi = "http://192.168.34.62/wcm-api/site/static/";
        }
    }


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
                    SiteMenuUtils.setMenuPages(sitePageList);
                    SiteMenuUtils.setLangList(langList);
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
    public DomainHtmlVo getFromApi(RestTemplate restTemplate, String path,String host) {
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
                    handleSchemaJson(model);
                    handleDateKey(model);
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
                        model.put("websiteUrl", website);
                    }
                    if (!model.isEmpty()) {
                        String content = templateEnginePageRenderer.renderPage(model.get("templateName").toString(), model);
                        DomainHtmlVo domainHtmlVo = new DomainHtmlVo();
                        domainHtmlVo.setHtmlContent(content);
                        domainHtmlVo.setModelMap(model);
                        if (model.containsKey("pageData")) {
                            Map<String, Object> pageMap = (Map<String, Object>) model.get("pageData");
                            domainHtmlVo.setTotalPages(Integer.parseInt(pageMap.get("totalPages").toString()));
                        }
                        return domainHtmlVo;
                    }
                } else {
                    log.error("request resp code:{},msg:{}", result.getCode(), result.getMsg());
                }
            }
            log.error("request {} error: {}", path, responseEntity.getStatusCode());
        } catch (Exception e) {
            log.error("request {} exception", path, e);
            return new DomainHtmlVo();
        }
        return new DomainHtmlVo();
    }

    private void handleSchemaJson(Map<String, Object> model) {
        for (String key : model.keySet()) {
            if (key.toLowerCase().endsWith("schemajson") &&
                    (model.get(key) instanceof Map || model.get(key) instanceof List)) {
                String newVal = JSON.toJSONString(model.get(key));
                model.put(key, newVal);
            }
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss+08:00");

    List<String> dateList = Arrays.asList("created", "updated");

    private void handleDateKey(Map<String, Object> model) {
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
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
                handleDateKey(nestedMap);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapInList = (Map<String, Object>) item;
                        handleDateKey(mapInList);
                    }
                }
            }
        }
    }


    @SneakyThrows
    public ResponseEntity<String> forwardFormRequest(HttpServletRequest originalRequest, String targetUrl) {
        StringBuilder formBody = new StringBuilder();
        Enumeration<String> parameterNames = originalRequest.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = originalRequest.getParameterValues(paramName);
            for (String value : paramValues) {
                if (formBody.length() > 0) {
                    formBody.append("&");
                }
                formBody.append(URLEncoder.encode(paramName, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(value, "UTF-8"));
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Deploy-Domain", originalRequest.getHeader("Host"));
        headers.add("Premium-Real-IP", originalRequest.getHeader("X-Real-IP"));
        HttpEntity<String> requestEntity = new HttpEntity<>(formBody.toString(), headers);
        return restTemplate.exchange(wcmApi + targetUrl, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * 首页
     *
     * @return 返回首页html
     */
    public String getHome(RestTemplate restTemplate,String host) {
        log.info("request Home data");
        try {
            return getFromApi(restTemplate, "home",host).getHtmlContent();
        } catch (Exception e) {
            log.error("request Home data exception", e);
            return "";
        }
    }
}