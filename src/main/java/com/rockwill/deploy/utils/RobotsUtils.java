package com.rockwill.deploy.utils;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@Slf4j
public class RobotsUtils {
    public static void generateRobots(String domain, boolean isHttps, String websiteRoot) {
        log.info("generate  Robots:{}", domain);
        String file = websiteRoot + "/robots.txt";
        try {
            Files.write(Paths.get(file), Arrays.asList("User-agent: *", "Sitemap: " + (isHttps ? "https://" : "http://") + domain + "/sitemap.xml"),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("generate  Robots failed :{}", e.getMessage(), e);
        }
    }
}
