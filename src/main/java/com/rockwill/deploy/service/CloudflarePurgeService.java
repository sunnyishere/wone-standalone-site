package com.rockwill.deploy.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rockwill.deploy.conf.CloudflareProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
/**
 * Cloudflare 缓存清理服务。
 * 针对免费版接口限制，按批次提交 URL，并在批次之间增加间隔。
 */
public class CloudflarePurgeService {

    private static final int DEFAULT_MAX_BATCH_SIZE = 100;

    @Autowired
    private CloudflareProperties cloudflareProperties;

    @Autowired
    private RestTemplate restTemplate;

    private SleepStrategy sleepStrategy = millis -> {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting between Cloudflare purge requests", e);
        }
    };

    /**
     * 按 URL 列表执行缓存清理。
     * 会先去重，再按批次请求 Cloudflare；单批失败不会中断后续批次，但最终会返回整体是否全部成功。
     */
    public boolean purgeByUrls(String domain, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return false;
        }
        try {
            String zoneId = cloudflareProperties.getZoneIdByDomain(domain);
            String apiToken = cloudflareProperties.getApiTokenByDomain(domain);
            if (StringUtils.isBlank(zoneId) || StringUtils.isBlank(apiToken)) {
                log.warn("Cloudflare zoneId or apiToken is empty for domain: {}", domain);
                return false;
            }
            String apiUrl = cloudflareProperties.getApiUrl() + "/" + zoneId + "/purge_cache";
            List<String> normalizedUrls = normalizeUrls(urls);
            int batchSize = resolveBatchSize();
            long requestIntervalMs = Math.max(0L, cloudflareProperties.getPurgeRequestIntervalMs());
            int totalBatches = (normalizedUrls.size() + batchSize - 1) / batchSize;
            log.info("Cloudflare purge cache request, domain: {}, totalUrls: {}, batchSize: {}, totalBatches: {}",
                    domain, normalizedUrls.size(), batchSize, totalBatches);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            boolean allBatchesSucceeded = true;
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int fromIndex = batchIndex * batchSize;
                int toIndex = Math.min(fromIndex + batchSize, normalizedUrls.size());
                List<String> batchUrls = normalizedUrls.subList(fromIndex, toIndex);
                boolean batchSuccess = purgeBatch(apiUrl, headers, batchUrls, batchIndex + 1, totalBatches, domain);
                if (!batchSuccess) {
                    allBatchesSucceeded = false;
                }
                if (batchIndex < totalBatches - 1 && requestIntervalMs > 0L) {
                    sleepStrategy.sleep(requestIntervalMs);
                }
            }
            return allBatchesSucceeded;
        } catch (Exception e) {
            log.error("Cloudflare purge cache failed, domain: {}", domain, e);
            return false;
        }
    }

    public boolean purgeByUrl(String domain, String url) {
        return purgeByUrls(domain, Collections.singletonList(url));
    }


    /**
     * 执行单个批次的 Cloudflare purge 请求。
     * 单批异常会被记录并返回失败，避免影响后续批次继续执行。
     */
    private boolean purgeBatch(String apiUrl,
                               HttpHeaders headers,
                               List<String> batchUrls,
                               int batchNumber,
                               int totalBatches,
                               String domain) {
        try {
            Map<String, Object> body = Collections.singletonMap("files", batchUrls);
            HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(body), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            String responseBody = response.getBody();
            boolean success = response.getStatusCode().is2xxSuccessful() && isCloudflareSuccess(responseBody);
            log.info("Cloudflare purge batch response, domain: {}, batch: {}/{}, urls: {}, success: {}, body: {}",
                    domain, batchNumber, totalBatches, batchUrls.size(), success, responseBody);
            return success;
        } catch (Exception e) {
            log.error("Cloudflare purge batch failed, domain: {}, batch: {}/{}, urls: {}",
                    domain, batchNumber, totalBatches, batchUrls.size(), e);
            return false;
        }
    }

    /**
     * 解析 Cloudflare 响应体中的 success 字段，避免只根据 HTTP 2xx 误判请求成功。
     */
    private boolean isCloudflareSuccess(String responseBody) {
        if (StringUtils.isBlank(responseBody) || !JSON.isValidObject(responseBody)) {
            return false;
        }
        JSONObject jsonObject = JSON.parseObject(responseBody);
        return Boolean.TRUE.equals(jsonObject.getBoolean("success"));
    }

    /**
     * 清洗并去重 URL，保证请求体中不会携带空值和重复项。
     */
    private List<String> normalizeUrls(List<String> urls) {
        Set<String> normalizedSet = new LinkedHashSet<>();
        for (String url : urls) {
            if (StringUtils.isNotBlank(url)) {
                normalizedSet.add(url.trim());
            }
        }
        return new ArrayList<>(normalizedSet);
    }

    /**
     * 根据配置计算实际批次大小，并强制限制在免费版允许的上限以内。
     */
    private int resolveBatchSize() {
        return Math.max(1, Math.min(cloudflareProperties.getPurgeBatchSize(), DEFAULT_MAX_BATCH_SIZE));
    }

    /**
     * 等待策略抽象，方便测试时替换真实 sleep。
     */
    interface SleepStrategy {
        void sleep(long millis);
    }

}
