package com.rockwill.deploy.utils;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class RobotsUtils {
    private static final List<String> AI_BOT_RULES = Arrays.asList(
            "User-agent: *",
            "Allow: /",
            "Disallow: /cdn-cgi/",
            "Disallow: /search/",
            "Disallow: /search-",
            ""
    );

    public static void generateRobots(String domain, boolean isHttps, String websiteRoot) {
        log.info("generate robots: {}", domain);
        Path robotsPath = Paths.get(websiteRoot, "robots.txt");
        List<String> lines = new ArrayList<>(AI_BOT_RULES);
        lines.add("Sitemap: " + (isHttps ? "https://" : "http://") + domain + "/sitemap.xml");

        try {
            Files.write(robotsPath, lines,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("generate robots failed: {}", e.getMessage(), e);
        }
    }
}
