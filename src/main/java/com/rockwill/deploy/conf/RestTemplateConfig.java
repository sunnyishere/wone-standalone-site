package com.rockwill.deploy.conf;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.Collections;

@Configuration
public class RestTemplateConfig {

    @Autowired
    RestTemplateHeaderInterceptor restTemplateHeaderInterceptor;

    @Resource
    HttpClientProperties httpClientProperties;


    @Bean("realTimeRestTemplate")
    public RestTemplate realTimeRestTemplate(RestTemplateBuilder builder) {
        return restTemplatePool(builder,
                httpClientProperties.getRealtimeConnectTimeout(),
                httpClientProperties.getRealtimeReadTimeout());
    }

    @Bean("jobRestTemplate")
    @Primary
    public RestTemplate jobRestTemplate(RestTemplateBuilder builder) {
        return restTemplatePool(builder,
                httpClientProperties.getJobConnectTimeout(),
                httpClientProperties.getJobReadTimeout());
    }

    private RestTemplate restTemplatePool(RestTemplateBuilder builder, int connectTimeout, int readTimeout) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(httpClientProperties.getPoolMaxTotal());
        connectionManager.setDefaultMaxPerRoute(httpClientProperties.getPoolMaxPerRoute());

        try {
            TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();

            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
                    sslContext,
                    new NoopHostnameVerifier()
            );

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(connectTimeout * 1000)
                            .setSocketTimeout(readTimeout * 1000)
                            .setConnectionRequestTimeout(httpClientProperties.getConnectionRequestTimeout() * 1000)
                            .build())
                    .build();

            HttpComponentsClientHttpRequestFactory factory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(connectTimeout * 1000);
            factory.setReadTimeout(readTimeout * 1000);

            RestTemplate restTemplate = builder
                    .requestFactory(() -> factory)
                    .setConnectTimeout(Duration.ofSeconds(connectTimeout))
                    .setReadTimeout(Duration.ofSeconds(readTimeout))
                    .build();
            restTemplate.setInterceptors(Collections.singletonList(restTemplateHeaderInterceptor));

            return restTemplate;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
