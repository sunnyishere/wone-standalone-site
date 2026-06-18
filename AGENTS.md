# AGENTS.md

## 项目概述

**wone-standalone-site** 是一个基于 Spring Boot 2.6.13（Java 8）的静态网站生成与部署服务。它从后端 CMS API 获取内容，通过 Thymeleaf 模板引擎渲染为 HTML，并将输出保存为静态文件，由 Nginx 对外提供服务。

- **GroupId**: `com.rockwill`
- **ArtifactId**: `wone-standalone-site`
- **Version**: `1.0.0`
- **主启动类**: [StandAloneSiteApplication](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/StandAloneSiteApplication.java)
- **默认端口**: `9080`

## 技术栈

| 类别           | 技术                               |
|----------------|------------------------------------|
| 框架           | Spring Boot 2.6.13                 |
| Java 版本      | 1.8                                |
| 模板引擎       | Thymeleaf（SpringTemplateEngine）  |
| 构建工具       | Maven                              |
| HTTP 客户端    | RestTemplate、Apache HttpClient    |
| JSON 处理      | fastjson2 2.0.43、Jackson          |
| HTML 解析      | Jsoup 1.10.2                       |
| 站点地图       | sitemapgen4j 1.1.1                 |
| 工具库         | Lombok、hutool-crypto、commons-lang3 |
| CDN 缓存清理   | Cloudflare API                     |
| 测试           | JUnit、spring-test                 |

## 项目结构

```
src/main/java/com/rockwill/deploy/
├── StandAloneSiteApplication.java      # 应用入口
├── conf/                                # 配置类
│   ├── ApiSignatureInterceptor.java     # API 签名鉴权拦截器
│   ├── AppStartListener.java            # 应用启动监听器
│   ├── ApplicationContextHolder.java    # Spring 上下文持有工具
│   ├── AsyncConfig.java                 # 异步任务线程池配置
│   ├── BrandConfig.java                 # 品牌/域名配置属性
│   ├── CachingRequestBodyFilter.java    # 请求体缓存过滤器
│   ├── CloudflareProperties.java        # Cloudflare API 配置属性
│   ├── GoogleTagProperties.java         # Google Tag Manager 配置属性
│   ├── MyUtilsDialect.java              # 自定义 Thymeleaf 方言
│   ├── PathMatchingInterceptor.java     # URL 路径匹配与校验拦截器
│   ├── RestTemplateConfig.java          # RestTemplate Bean 配置
│   ├── RestTemplateHeaderInterceptor.java # RestTemplate 请求头拦截器
│   ├── RouteExceptionHandler.java       # 全局异常处理
│   ├── ThymeleafConfig.java             # Thymeleaf 引擎配置
│   └── WebConfig.java                   # Spring MVC 配置（拦截器、CORS）
├── controller/                          # REST 控制器
│   ├── StaticizeTriggerController.java  # 静态化触发 API（/api/staticize/**）
│   └── UnifiedRouterController.java     # 统一页面请求路由（/page/**）
├── render/                              # 渲染层
│   └── TemplateEnginePageRenderer.java  # 基于 Thymeleaf 的 HTML 页面渲染器
├── scheduler/                           # 定时任务
│   └── StaticPageScheduler.java         # 每日定时全量静态页面生成
├── service/                             # 业务服务
│   ├── CloudflarePurgeService.java      # Cloudflare CDN 缓存清理
│   ├── RockwillKnowledgeService.java    # 后端 API 客户端（内容与菜单）
│   └── StaticPageService.java           # 核心静态页面生成引擎
├── utils/                               # 工具类
│   ├── AccessRecord.java                # 访问记录追踪
│   ├── PathMatchUtils.java              # URL 路径正则匹配（类 Nginx 规则）
│   ├── PathPatternType.java             # URL 路径类型枚举
│   ├── RobotsUtils.java                 # robots.txt 生成
│   ├── SiteMenuUtils.java               # 站点菜单数据持有
│   ├── SiteSitemapUtils.java            # sitemap.xml 生成
│   └── ThymeleafUtils.java              # Thymeleaf 辅助工具
└── vo/                                  # 值对象 / DTO
    ├── AjaxResult.java                  # 通用 API 响应包装
    ├── DomainHtmlVo.java                # 域名 + HTML 内容 VO
    ├── SecurityReq.java                 # API 安全请求参数
    ├── SitePage.java                    # 站点页面实体（含 SitePageType 常量）
    ├── StandaloneSyncAction.java        # 同步动作枚举（CREATE/UPDATE/DELETE）
    ├── StandaloneSyncEntityType.java    # 同步实体类型枚举（PRODUCT/SOLUTION/NEWS 等）
    └── StandaloneSyncEvent.java         # 同步事件模型

src/main/resources/
├── application.yml                      # 应用配置
├── logback.xml                          # 日志配置
├── templates/                           # Thymeleaf HTML 模板（25 个模板）
└── static/                              # 静态资源（css、js、fonts、images）
```

## 核心架构

### 1. 页面请求流程（UnifiedRouterController）

```
浏览器请求 → Nginx → /page/** → PathMatchingInterceptor → UnifiedRouterController
                                                                       ↓
                                                            PathPatternType 类型匹配
                                                                       ↓
                                              RockwillKnowledgeService → 后端 CMS API
                                                                       ↓
                                                         Thymeleaf 渲染 → HTML
                                                                       ↓
                                                      StaticPageService.saveHtml()
                                                                       ↓
                                                              返回响应给浏览器
```

[UnifiedRouterController](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/controller/UnifiedRouterController.java) 处理所有 `/page/**` 的页面请求，具体流程：
- 从 [PathMatchingInterceptor](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/conf/PathMatchingInterceptor.java) 接收转发目标
- 根据 [PathPatternType](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/utils/PathPatternType.java) 路由到不同处理逻辑（DETAIL、SEARCH、CATEGORY_PAGINATION 等）
- 通过 `RockwillKnowledgeService` 向后端 CMS API 获取渲染后的 HTML
- 通过 `StaticPageService.saveHtml()` 持久化为静态文件
- 返回 HTML 响应

### 2. URL 路径匹配（PathMatchUtils）

[PathMatchUtils](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/utils/PathMatchUtils.java) 使用正则表达式将请求 URL 匹配到已知路由类型（类似 Nginx location 规则）：

| 匹配类型 | 示例 URL | 后端 API 目标 |
|---|---|---|
| `SEARCH` | `/search/categoryName-keyword-catId-pageNum` | `/public/search` |
| `DETAIL` | `/products/detail/title-name-123` | `/public/detail` |
| `CATEGORY_PAGINATION` | `/products/categoryName-456-2` | `/public/urlData` |
| `MULTI_LEVEL` | `/menu/subpath-789` | `/public/url` |
| `MENU_WITH_PAGE` | `/products-2` | `/menu/topNavPages` |
| `MENU_WITHOUT_PAGE` | `/products` | `/menu/topNavPages` |
| `LEAVE_MESSAGE` | `/leaveMessage` | `/public/leaveMessage` |

### 3. 静态页面生成（StaticPageService）

[StaticPageService](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/service/StaticPageService.java) 是核心引擎，功能包括：
- **全量生成**（`generateAllPages`）：为指定域名生成所有页面，包括首页、菜单页面、详情页、sitemap 和 robots.txt
- **增量生成**（`triggerGenPages`）：仅生成发生变更的实体 ID 对应的页面
- 使用 `RockwillKnowledgeService` 从后端 CMS API 获取内容
- 使用 [TemplateEnginePageRenderer](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/render/TemplateEnginePageRenderer.java) 结合 Thymeleaf 渲染 HTML
- 输出文件到配置的 `static-output` 目录（默认 `./static-output`）
- 支持多域名，每个域名拥有独立子目录
- 支持 60+ 种语言，按语言生成页面
- 生成 `sitemap.xml` 和 `robots.txt`

### 4. 触发与同步 API（StaticizeTriggerController）

[StaticizeTriggerController](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/controller/StaticizeTriggerController.java) 暴露两个端点：

- **`POST /api/staticize/trigger`**：手动触发静态化生成。支持全站生成或按实体类型（产品/文章/案例/新闻）指定 ID 增量生成。
- **`POST /api/staticize/sync`**：处理 CMS 同步事件。当实体发生 CREATE/UPDATE 时，删除受影响的静态文件并将对应 URL 加入 Cloudflare 缓存清理队列。

两个端点均受 [ApiSignatureInterceptor](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/conf/ApiSignatureInterceptor.java) 保护，采用 MD5 签名校验，时间戳有效期 5 分钟。

### 5. Cloudflare 集成

[CloudflarePurgeService](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/service/CloudflarePurgeService.java) 按 URL 清理 CDN 缓存：
- 分批请求（默认每批 100 个 URL），批次间可配置间隔
- 按域名配置 Zone ID 和 API Token
- 自动去重和 URL 规范化

### 6. 定时任务

[StaticPageScheduler](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/scheduler/StaticPageScheduler.java) 按配置的 cron 表达式（默认 `0 0 0,12 * * ?`，即每天 0 点和 12 点）自动重新生成所有静态页面。可通过 `brand.scheduler-enable` 关闭。

## 关键配置（application.yml）

| 配置项 | 说明 |
|---|---|
| `server.port` | 应用端口（默认 9080） |
| `wcm-api` | 后端 CMS API 基础 URL |
| `brand.domain` | 主域名 |
| `brand.static-output` | 静态 HTML 输出目录 |
| `brand.languages` | 支持的语言代码列表，逗号分隔（60+） |
| `brand.scheduler-enable` | 是否启用定时生成 |
| `brand.cron` | 定时生成的 cron 表达式 |
| `brand.execute-on-start` | 启动时是否执行全量生成 |
| `brand.secret` | API 签名密钥 |
| `brand.domain-list` | 额外需要生成的域名列表 |
| `cloudflare.domains.*.zone-id` | 各域名对应的 Cloudflare Zone ID |
| `cloudflare.domains.*.api-token` | 各域名对应的 Cloudflare API Token |
| `cdn.enabled` | CDN 模式开关 |
| `google-tag.id` | 默认 Google Analytics 跟踪 ID |

## 开发指南

### 构建
```bash
mvn clean package
```
输出：`target/wone-standalone-site.jar`

### 运行
```bash
java -jar target/wone-standalone-site.jar
```
或通过 Spring Boot Maven 插件：
```bash
mvn spring-boot:run
```

### 多域名支持
- 主域名通过 `brand.domain` 配置
- 额外域名通过 `brand.domain-list` 添加（YAML 列表格式）
- 每个域名在 `static-output` 下拥有独立的子目录
- Cloudflare 和 Google Tag 配置均按域名区分

### 多语言支持
- `brand.languages` 和 `brand.mapLang` 中配置了 60+ 种语言
- 语言代码映射（如 `en` → `en_US`）用于本地化渲染
- 全量生成时会按语言分别生成页面

### 静态文件输出
- 静态文件写入 `./static-output/{domain}/`（可配置）
- 包含首页 `index.html` 和 404 错误页 `404.html`
- 按菜单创建目录，内含详情页和分页子目录
- 域名根目录下生成 `sitemap.xml` 和 `robots.txt`

### API 签名验证
所有 `/api/staticize/**` 下的端点都需要以下参数：
- `appId`、`timestamp`、`nonce`、`signature`、`data`
- MD5 签名算法：`MD5(appId={}&timestamp={}&nonce={}&data={}&secret={})`
- 时间戳与当前时间差不得超过 5 分钟

### 拦截器链
1. `CachingRequestBodyFilter` — 缓存请求体以支持重复读取（作用于 `/api/*`）
2. `PathMatchingInterceptor` — 校验 URL 路径是否符合已知规则（作用于 `/page/**`）
3. `ApiSignatureInterceptor` — 校验 API 签名（作用于 `/api/staticize/**`）

### 模板引擎
- Thymeleaf 模板位于 `src/main/resources/templates/`（共 25 个 HTML 模板）
- 自定义方言：[MyUtilsDialect](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/conf/MyUtilsDialect.java)
- 模板包括：`index`、`products`、`solutions`、`successcase`、`new`、`search`、`about`、`inquiry`、`documents`、各详情页、分页、404 以及 header/footer 片段

### SitePage 页面类型
定义在 [SitePage.SitePageType](file:///D:/repo/deployService/wone-standalone-site/src/main/java/com/rockwill/deploy/vo/SitePage.java)：
- `HOME`（1-首页）、`PRODUCTS`（2-产品）、`SOLUTIONS`（3-解决方案）、`DOCUMENTS`（4-文档）、`NEWS`（5-新闻）、`PROFILE`（6-公司简介）、`SUCCESS_REFERENCE`（7-成功案例）