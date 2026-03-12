package com.rockwill.deploy.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.service.StaticPageService;
import com.rockwill.deploy.vo.SecurityReq;
import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/api/staticize")
public class StaticizeTriggerController {

    @Autowired
    StaticPageService staticPageService;

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerStaticization(@ModelAttribute SecurityReq request) {
        log.info("Received publish : {}", request.getData());
        String domain = "";
        Map<Integer, Set<Long>> resultMap = new HashMap<>();
        if (JSON.isValid(request.getData().toString())) {
            JSONObject object = JSON.parseObject(request.getData().toString());
            domain = object.getString("domain");
            if (object.containsKey("prodIds")) {
                resultMap.put(SitePage.SitePageType.PRODUCTS, Arrays.stream(object.getString("prodIds")
                        .split(",")).filter(StringUtils::hasText).map(Long::parseLong).collect(Collectors.toSet()));
            }
            if (object.containsKey("articleIds")) {
                resultMap.put(SitePage.SitePageType.SOLUTIONS, Arrays.stream(object.getString("articleIds")
                        .split(",")).filter(StringUtils::hasText).map(Long::parseLong).collect(Collectors.toSet()));
            }
            if (object.containsKey("caseIds")) {
                resultMap.put(SitePage.SitePageType.SUCCESS_REFERENCE, Arrays.stream(object.getString("caseIds")
                        .split(",")).filter(StringUtils::hasText).map(Long::parseLong).collect(Collectors.toSet()));
            }
            if (object.containsKey("newsIds")) {
                resultMap.put(SitePage.SitePageType.NEWS, Arrays.stream(object.getString("newsIds")
                        .split(",")).filter(StringUtils::hasText).map(Long::parseLong).collect(Collectors.toSet()));
            }
        } else {
            domain = request.getData().toString();
        }
        staticPageService.triggerGenPages(domain, resultMap);
        return ResponseEntity.ok("Static page generated accepted");
    }
}