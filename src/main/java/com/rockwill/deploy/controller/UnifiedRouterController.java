package com.rockwill.deploy.controller;

import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.service.RockwillKnowledgeService;
import com.rockwill.deploy.service.StaticPageService;
import com.rockwill.deploy.utils.PathPatternType;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.vo.DomainHtmlVo;
import com.rockwill.deploy.vo.SitePage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String host = request.getHeader("Host");
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
            case DETAIL:
            case CATEGORY_PAGINATION:
            case MULTI_LEVEL:
            case CATEGORY_WITH_ID:
            case MENU_WITH_PAGE:
            case SEARCH:
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
        if (ObjectUtils.isEmpty(domainHtmlVo.getHtmlContent())) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/"))
                    .headers(headers)
                    .build();
        }
        //静态化未存储，及时保存html文件
        if (!realUri.startsWith("/search")) {
            String savePrefix = realUri.substring(1);
            staticPageService.saveHtml(host,savePrefix, domainHtmlVo.getHtmlContent());
        }
        return new ResponseEntity<>(domainHtmlVo.getHtmlContent(), headers, HttpStatus.OK);
    }

}