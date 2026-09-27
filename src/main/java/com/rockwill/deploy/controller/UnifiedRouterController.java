package com.rockwill.deploy.controller;

import com.rockwill.deploy.service.RockwillKnowledgeService;
import com.rockwill.deploy.service.StaticPageService;
import com.rockwill.deploy.utils.PathPatternType;
import com.rockwill.deploy.vo.DomainHtmlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;


/**
 *
 *
 * 访问路径不存在时，统一处理：
 * 1.访问未及时静态化页面，实时转发请求响应，并静态化
 * 2.其他非规则内的访问路径，重定向静态化首页
 */
@RestController
@Slf4j
public class UnifiedRouterController {

    @Autowired
    private RockwillKnowledgeService rockwillKnowledgeService;

    @Autowired
    @Qualifier("realTimeRestTemplate")
    private RestTemplate realTimeRestTemplate;

    @Autowired
    StaticPageService staticPageService;

    @RequestMapping(value = "/page/**", produces = {"text/html"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> routeRequest(HttpServletRequest request) throws IOException {
        String realUri = "";
        if (request.getAttribute("X-Original-URI") != null) {
            realUri = request.getAttribute("X-Original-URI").toString();
        } else {
            realUri = request.getHeader("X-Original-URI");
        }
        realUri = realUri.substring(5);
        //优先取拦截器归一后的配置域名，保证与菜单缓存、静态文件目录、CMS Deploy-Domain 口径一致
        Object resolvedHost = request.getAttribute("resolvedHost");
        String host = resolvedHost != null ? resolvedHost.toString() : request.getHeader("Host");
        log.info("request url : {}", realUri);
        Object patternType = request.getAttribute("patternType");
        Object forwardTarget = request.getAttribute("forwardTarget");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        DomainHtmlVo domainHtmlVo;
        PathPatternType pathPatternType = PathPatternType.fromString(patternType.toString());
        switch (pathPatternType) {
            case LEAVE_MESSAGE:
            case UPLOAD:
                ResponseEntity<String> response = rockwillKnowledgeService.forwardFormRequest(request, forwardTarget.toString());
                return ResponseEntity.status(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(response.getBody());
            case MENU_WITHOUT_PAGE:
                if (realUri.equals("/")) {
                    domainHtmlVo = rockwillKnowledgeService.getHome(realTimeRestTemplate, host);
                    break;
                }
//                if (isSearchPrefixRequest(forwardTarget.toString())) {
//                    // /search-xxx 形态：关键词本地搜索
//                    String keyword = extractParam(forwardTarget.toString(), "name");
//                    keyword = keyword != null && keyword.startsWith("search-")
//                            ? keyword.substring("search-".length()) : "";
//                    domainHtmlVo = rockwillKnowledgeService.searchSiteContent(keyword, null, "1", null, host);
//                    break;
//                }
            case DETAIL:
            case CATEGORY_PAGINATION:
            case MULTI_LEVEL:
            case CATEGORY_WITH_ID:
            case MENU_WITH_PAGE:
            case SEARCH:
//                if (pathPatternType == PathPatternType.SEARCH) {
//                    // /search/xxx-xxx-id-page 形态：分类+关键词本地搜索
//                    domainHtmlVo = rockwillKnowledgeService.searchSiteContent(
//                            extractParam(forwardTarget.toString(), "searchKey"),
//                            extractParam(forwardTarget.toString(), "categoryId"),
//                            extractParam(forwardTarget.toString(), "pageNum"),
//                            extractParam(forwardTarget.toString(), "lang"),
//                            host);
//                    break;
//                }
                domainHtmlVo = rockwillKnowledgeService.getFromApi(realTimeRestTemplate, forwardTarget.toString(),host);
                break;
            case DEFAULT:
            default:
                log.error("Unsupported pattern type,request:{}", realUri);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .headers(headers)
                        .build();
        }
        HttpStatus status = HttpStatus.OK;
        if (ObjectUtils.isEmpty(domainHtmlVo.getHtmlContent())) {
            if (domainHtmlVo.getHttpErrCode() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .headers(headers)
                        .build();
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/"))
                    .headers(headers)
                    .build();
        }
        if (domainHtmlVo.getHttpErrCode() == HttpStatus.NOT_FOUND.value()) {
            status = HttpStatus.NOT_FOUND;
        }
        //静态化未存储，及时保存html文件
        if (!realUri.startsWith("/search") && status != HttpStatus.NOT_FOUND) {
            String savePrefix = realUri.substring(1);
            staticPageService.saveHtml(host,savePrefix, domainHtmlVo.getHtmlContent());
        }
        return new ResponseEntity<>(domainHtmlVo.getHtmlContent(), headers, status);
    }

    /**
     * 判断 MENU_WITHOUT_PAGE 转发目标是否为 /search-xxx 搜索请求（PathMatchUtils 特判产出）
     */
    private boolean isSearchPrefixRequest(String forwardTarget) {
        String name = extractParam(forwardTarget, "name");
        return name != null && name.startsWith("search-");
    }

    /**
     * 从转发目标查询串中提取指定参数值（查询串形如 /xxx?a=1&b=2）
     */
    private String extractParam(String forwardTarget, String param) {
        if (forwardTarget == null) {
            return null;
        }
        int queryIndex = forwardTarget.indexOf('?');
        if (queryIndex < 0) {
            return null;
        }
        for (String pair : forwardTarget.substring(queryIndex + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(param)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

}