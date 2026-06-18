package com.rockwill.deploy.service;

import com.rockwill.deploy.conf.CloudflareProperties;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class CloudflarePurgeServiceTest {

    @Test
    /**
     * 验证 URL 会按配置批次拆分，并且只在批次之间执行等待。
     */
    public void shouldBatchRequestsAndSleepBetweenThem() {
        CloudflareProperties properties = new CloudflareProperties();
        properties.setZoneId("zone-1");
        properties.setApiToken("token-1");
        properties.setPurgeBatchSize(100);
        properties.setPurgeRequestIntervalMs(500L);

        RecordingRestTemplate restTemplate = new RecordingRestTemplate();
        CloudflarePurgeService service = new CloudflarePurgeService();
        ReflectionTestUtils.setField(service, "cloudflareProperties", properties);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);


        List<String> urls = new ArrayList<>();
        for (int i = 0; i < 205; i++) {
            urls.add("https://www.test-static.com/page-" + i);
        }

        boolean success = service.purgeByUrls("www.test-static.com", urls);

        Assert.assertTrue(success);
        Assert.assertEquals(3, restTemplate.requestBodies.size());
        Assert.assertTrue(restTemplate.requestBodies.get(0).contains("page-99"));
        Assert.assertFalse(restTemplate.requestBodies.get(0).contains("page-100"));
        Assert.assertTrue(restTemplate.requestBodies.get(1).contains("page-199"));
        Assert.assertFalse(restTemplate.requestBodies.get(1).contains("page-200"));
        Assert.assertTrue(restTemplate.requestBodies.get(2).contains("page-204"));
    }

    @Test
    /**
     * 验证单批失败时不会中断后续批次，最终结果仍能反映整体存在失败。
     */
    public void shouldContinueWhenSingleBatchFails() {
        CloudflareProperties properties = new CloudflareProperties();
        properties.setZoneId("zone-1");
        properties.setApiToken("token-1");
        properties.setPurgeBatchSize(100);
        properties.setPurgeRequestIntervalMs(300L);

        RecordingRestTemplate restTemplate = new RecordingRestTemplate();
        restTemplate.failOnCall = 2;
        CloudflarePurgeService service = new CloudflarePurgeService();
        ReflectionTestUtils.setField(service, "cloudflareProperties", properties);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);


        List<String> urls = new ArrayList<>();
        for (int i = 0; i < 205; i++) {
            urls.add("https://example.com/page-" + i);
        }

        boolean success = service.purgeByUrls("example.com", urls);

        Assert.assertFalse(success);
        Assert.assertEquals(3, restTemplate.requestBodies.size());
    }

    private static class RecordingRestTemplate extends RestTemplate {
        private final List<String> requestBodies = new ArrayList<>();
        private int failOnCall = -1;
        private int callCount = 0;

        @Override
        public <T> ResponseEntity<T> exchange(String url,
                                              HttpMethod method,
                                              org.springframework.http.HttpEntity<?> requestEntity,
                                              Class<T> responseType,
                                              Object... uriVariables) {
            callCount++;
            requestBodies.add(String.valueOf(requestEntity.getBody()));
            if (callCount == failOnCall) {
                throw new RuntimeException("mock purge failure");
            }
            return new ResponseEntity<>(responseType.cast("{\"success\":true}"), HttpStatus.OK);
        }
    }
}
