package com.rockwill.deploy.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.conf.BrandConfig;
import com.rockwill.deploy.service.CloudflarePurgeService;
import com.rockwill.deploy.service.StaticPageService;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 静态化触发入口。
 * 既负责常规静态页生成，也负责站点同步事件触发后的本地文件删除与 Cloudflare 缓存清理。
 */
@RestController
@Slf4j
@RequestMapping("/api/staticize")
public class StaticizeTriggerController {

    @Autowired
    StaticPageService staticPageService;

    @Resource
    BrandConfig brandConfig;

    @Autowired
    CloudflarePurgeService cloudflarePurgeService;


    /**
     * 手动触发静态化生成。
     * 支持全站生成，也支持按产品、文章、案例、新闻等指定 ID 增量生成。
     */
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

    /**
     * 处理独立站同步事件。
     * 根据同步实体类型定位对应静态目录，删除受影响页面，并将对应 URL 加入 Cloudflare purge 队列。
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync(@ModelAttribute SecurityReq request,
                                              @RequestAttribute(name = "parsedData", required = false) JSONObject parsedData) {
        if (parsedData != null) {
            log.info("trigger sync: {}", parsedData);
            StandaloneSyncEvent standaloneSyncEvent = JSON.parseObject(parsedData.toString(), StandaloneSyncEvent.class);
            if (standaloneSyncEvent.getAction() == StandaloneSyncAction.DELETE) {
                log.warn("ignore delete event: {}", standaloneSyncEvent);
                return ResponseEntity.ok("Static page generated accepted");
            }
            SyncContext syncContext = buildSyncContext(standaloneSyncEvent);
            if (syncContext == null) {
                return ResponseEntity.ok("Static page generated accepted");
            }
            Set<String> urlSet = new LinkedHashSet<>();
            collectHomePage(syncContext, standaloneSyncEvent, urlSet);
            collectPaginationPages(syncContext, standaloneSyncEvent, urlSet);
            collectMainPages(syncContext, standaloneSyncEvent, urlSet);
            cloudflarePurgeService.purgeByUrls(standaloneSyncEvent.getDeployDomain(), new ArrayList<>(urlSet));
        }
        return ResponseEntity.ok("success");
    }

    /**
     * 根据同步事件构建当前页面类型的上下文信息，便于后续统一查找和删除静态文件。
     */
    private SyncContext buildSyncContext(StandaloneSyncEvent standaloneSyncEvent) {
        StandaloneSyncEntityType entityType = standaloneSyncEvent.getEntityType();
        for (SitePage sitePage : SiteMenuUtils.getMenuPages()) {
            if (entityType.getPageType() == sitePage.getPageType().intValue()) {
                String pageName = sitePage.getPageName();
                String domainPrefix = standaloneSyncEvent.getDeployDomain().equals(brandConfig.getDomain())
                        ? ""
                        : "/" + standaloneSyncEvent.getDeployDomain();
                File rootPath = new File(brandConfig.getStaticOutput() + domainPrefix);
                File pagePath = new File(rootPath, pageName);
                return new SyncContext(
                        pageName,
                        sitePage.getPageType() == SitePage.SitePageType.PRODUCTS,
                        rootPath,
                        pagePath
                );
            }
        }
        return null;
    }

    /**
     * 收集站点首页文件。
     * 同步事件发生后，首页通常会引用最新内容，因此需要删除根目录下的 index.html 并触发缓存清理。
     */
    private void collectHomePage(SyncContext syncContext,
                                 StandaloneSyncEvent standaloneSyncEvent,
                                 Set<String> urlSet) {
        File homePage = new File(syncContext.rootPath, "index.html");
        deleteAndRecord(homePage, standaloneSyncEvent.getDeployDomain(), "/", urlSet);
        staticPageService.generateIndexPage(standaloneSyncEvent.getDeployDomain());
    }

    /**
     * 收集当前栏目下的分页目录。
     * 同步后分页内容可能整体失效，因此这里按栏目统一删除并记录待 purge URL。
     */
    private void collectPaginationPages(SyncContext syncContext,
                                        StandaloneSyncEvent standaloneSyncEvent,
                                        Set<String> urlSet) {
        if (!syncContext.rootPath.exists()) {
            return;
        }
        File[] paginationDirs = syncContext.rootPath.listFiles(pathname -> pathname.isDirectory()
                && pathname.getName().startsWith(syncContext.pageName + "-"));
        if (paginationDirs == null) {
            return;
        }
        for (File file : paginationDirs) {
            deleteAndRecord(file, standaloneSyncEvent.getDeployDomain(), "/" + file.getName(), urlSet);
        }
    }

    /**
     * 收集当前栏目下的主页面和详情目录。
     * 非产品类按实体 ID 匹配目录；产品类额外处理系列页和 detail 目录。
     */
    private void collectMainPages(SyncContext syncContext,
                                  StandaloneSyncEvent standaloneSyncEvent,
                                  Set<String> urlSet) {
        File[] files = syncContext.pagePath.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (isIndexFile(file)) {
                deleteAndRecord(file, standaloneSyncEvent.getDeployDomain(), "/" + syncContext.pageName, urlSet);
                continue;
            }
            if (!file.isDirectory()) {
                continue;
            }
            if (syncContext.isProd) {
                collectProductPages(file, syncContext, standaloneSyncEvent, urlSet);
            } else if (file.getName().contains("-" + standaloneSyncEvent.getEntityId())) {
                deleteAndRecord(file, standaloneSyncEvent.getDeployDomain(),
                        "/" + syncContext.pageName + "/" + file.getName(), urlSet);
            }
        }
    }

    /**
     * 处理产品类页面的删除规则。
     * 包括系列页、detail 详情页以及普通产品目录页。
     */
    private void collectProductPages(File file,
                                     SyncContext syncContext,
                                     StandaloneSyncEvent standaloneSyncEvent,
                                     Set<String> urlSet) {
        String fileName = file.getName();
        Long entityId = standaloneSyncEvent.getEntityId();
        if (fileName.contains("-" + entityId + "-series")) {
            deleteAndRecord(file, standaloneSyncEvent.getDeployDomain(),
                    "/" + syncContext.pageName + "/" + fileName, urlSet);
            return;
        }
        if ("detail".equals(fileName)) {
            File[] detailFiles = file.listFiles(pathname -> pathname.getName().contains("-" + entityId));
            if (detailFiles == null) {
                return;
            }
            for (File detailFile : detailFiles) {
                deleteAndRecord(detailFile, standaloneSyncEvent.getDeployDomain(),
                        "/" + syncContext.pageName + "/detail/" + detailFile.getName(), urlSet);
            }
            return;
        }
        if (!fileName.contains("series")) {
            deleteAndRecord(file, standaloneSyncEvent.getDeployDomain(),
                    "/" + syncContext.pageName + "/" + fileName, urlSet);
        }
    }

    private boolean isIndexFile(File file) {
        return file.isFile() && "index.html".equals(file.getName());
    }

    /**
     * 删除本地静态文件或目录，并记录需要提交到 Cloudflare 的完整 URL。
     * 若传入域名不包含协议头，默认补全为 https。
     */
    private void deleteAndRecord(File file, String domain, String path, Set<String> urlSet) {
        if (file.exists()) {
            FileUtil.del(file);
            System.out.println("delete: " + file.getName());
            String normalizedDomain = domain.startsWith("http://") || domain.startsWith("https://")
                    ? domain
                    : "https://" + domain;
            urlSet.add(normalizedDomain + path);
        }
    }

    /**
     * 同步处理过程中使用的页面上下文。
     * 封装栏目名称、是否产品页以及本地静态目录位置。
     */
    private static class SyncContext {
        private final String pageName;
        private final boolean isProd;
        private final File rootPath;
        private final File pagePath;

        private SyncContext(String pageName, boolean isProd, File rootPath, File pagePath) {
            this.pageName = pageName;
            this.isProd = isProd;
            this.rootPath = rootPath;
            this.pagePath = pagePath;
        }
    }
}
