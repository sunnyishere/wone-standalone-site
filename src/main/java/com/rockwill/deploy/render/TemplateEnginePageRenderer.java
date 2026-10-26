package com.rockwill.deploy.render;

import com.rockwill.deploy.conf.BrandConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;


/**
 * 使用Thymeleaf TemplateEngine进行页面渲染
 *
 */
@Component
@Slf4j
public class TemplateEnginePageRenderer {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    BrandConfig brandConfig;

    private String staticDirName;

    @PostConstruct
    public void initDirName() {
        this.staticDirName = Paths.get(brandConfig.getStaticOutput()).toFile().getName();
    }

    /**
     * 使用Thymeleaf模板引擎渲染页面
     *
     * @param templateName 模板名称（位于templates目录下）
     * @param variables    模板变量
     * @return 渲染后的HTML内容
     */
    public synchronized String renderPage(String templateName, Map<String, Object> variables) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("Template name cannot be empty");
        }
        String resolvedTemplateName = resolveTemplateName(templateName, variables);
        Map<String, Object> contextVariables = new HashMap<>(variables);
        // 公共片段按自己的配置选择，不能继承主页面的 templateVersion。
        contextVariables.put("footerTemplate", resolveTemplateName("footer",
                Collections.singletonMap("templateVersions", variables.get("templateVersions") instanceof Map
                        ? variables.get("templateVersions") : Collections.emptyMap())));
        WebContext context = getWebContext(contextVariables);
        String htmlContent = templateEngine.process(resolvedTemplateName, context);
        if (htmlContent.contains("http://oss.iwone.cn")) {
            htmlContent = htmlContent.replaceAll("http://oss.iwone.cn", "https://oss.iee-business.com");
        }
        log.info("Template rendering completed: {}, size: {}KB", resolvedTemplateName, htmlContent.length() / 1024);
        return htmlContent;
    }

    /**
     * 仅切换页面入口；片段仍由模板中的显式名称解析，可直接共享根目录片段。
     * 未提供版本、非法版本或该版本尚无对应页面时，沿用原模板。
     */
    String resolveTemplateName(String templateName, Map<String, Object> variables) {
        Object configuredVersion = variables == null ? null : variables.get("templateVersion");
        Object templateVersions = variables == null ? null : variables.get("templateVersions");
        if (templateVersions instanceof Map) {
            // 本地搜索页、404 页可能复用其他页面的模型，必须按实际模板名选择。
            configuredVersion = ((Map<?, ?>) templateVersions).get(templateName);
        }
        if (configuredVersion == null) {
            return templateName;
        }
        String version = configuredVersion.toString().trim();
        if ("t1".equals(version) || version.isEmpty()) {
            return templateName;
        }
        if (!version.matches("t[1-9][0-9]*")) {
            log.warn("Invalid template version: {}, using default template", version);
            return templateName;
        }
        String candidate = version + "/" + templateName;
        return new ClassPathResource("templates/" + candidate + ".html").exists()
                ? candidate : templateName;
    }

    private WebContext getWebContext(Map<String, Object> variables) {
        HttpServletRequest request;
        HttpServletResponse response;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
            response = attributes.getResponse();
        } else {
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
        }
        ServletContext servletContext = webApplicationContext.getServletContext();
        return new WebContext(request, response, servletContext, Locale.ENGLISH, variables);
    }


    /**
     * 将渲染后的内容保存到文件
     *
     * @param content  HTML内容
     * @param filePath 文件路径
     * @throws IOException 文件操作异常
     */
    public void saveToFile(String content, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        log.info("Static page saved: {}", file.getAbsolutePath());
    }

}
