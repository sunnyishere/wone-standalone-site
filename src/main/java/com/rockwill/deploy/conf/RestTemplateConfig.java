package com.rockwill.deploy.conf;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.Collections;

@Configuration
public class RestTemplateConfig {

    @Autowired
    RestTemplateHeaderInterceptor restTemplateHeaderInterceptor;


    @Bean("realTimeRestTemplate")
    public RestTemplate realTimeRestTemplate(RestTemplateBuilder builder) {
        return restTemplate(builder,10);
    }

    @Bean("jobRestTemplate")
    @Primary
    public RestTemplate jobRestTemplate(RestTemplateBuilder builder) {
        return restTemplate(builder,120);
    }

    private RestTemplate restTemplate(RestTemplateBuilder builder,int timeout) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(timeout))
                .setReadTimeout(Duration.ofSeconds(timeout))
                .build();

        restTemplate.setInterceptors(Collections.singletonList(restTemplateHeaderInterceptor));
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = null;
        try {
            sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return restTemplate;
    }
}
