package com.rockwill.deploy.controller;

import com.rockwill.deploy.service.StaticPageService;
import com.rockwill.deploy.vo.SecurityReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/staticize")
public class StaticizeTriggerController {

    @Autowired
    StaticPageService staticPageService;

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerStaticization(@ModelAttribute SecurityReq  request) {
        log.info("Received publish : {}", request.getData());
        staticPageService.triggerGenPages(request.getData().toString());
        return ResponseEntity.ok("Static page generated successfully");
    }
}