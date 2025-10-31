package com.rockwill.deploy.render;

import com.rockwill.deploy.conf.BrandConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        try {
            if (templateName == null || templateName.trim().isEmpty()) {
                throw new IllegalArgumentException("Template name cannot be empty");
            }
            WebContext context = getWebContext(variables);
            String htmlContent = templateEngine.process(templateName, context);
            log.info("Template rendering completed: {}, size: {}KB", templateName, htmlContent.length() / 1024);
            return htmlContent;
        } catch (Exception e) {
            log.error("Rendering template failed: {}", templateName, e);
            return "";
        }
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