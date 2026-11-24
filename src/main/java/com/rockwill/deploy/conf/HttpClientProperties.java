package com.rockwill.deploy.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RestTemplate HTTP 客户端参数（连接池与超时），见 RestTemplateConfig。
 * 历史上这些值写死在配置类里：real-time 路径 connect/read 同为 10s，CMS 高负载渲染
 * 超过 10s 即抛 SocketTimeoutException；连接池 maxTotal=3 又把 wcm-api 并发许可（5）
 * 截断为 3。现改为可配置并放宽默认值，未配置时以下字段默认值兜底。
 */
@Component
@ConfigurationProperties(prefix = "http-client")
@Data
public class HttpClientProperties {

    /**
     * real-time 模板（页面按需渲染）连接超时（秒）
     */
    private Integer realtimeConnectTimeout = 5;

    /**
     * real-time 模板（页面按需渲染）读超时（秒）
     */
    private Integer realtimeReadTimeout = 30;

    /**
     * job 模板（后台静态化任务）连接超时（秒）
     */
    private Integer jobConnectTimeout = 120;

    /**
     * job 模板（后台静态化任务）读超时（秒）
     */
    private Integer jobReadTimeout = 120;

    /**
     * 从连接池租借连接的等待超时（秒），防止池耗尽时请求无限期挂起
     */
    private Integer connectionRequestTimeout = 5;

    /**
     * 连接池总连接数上限，不得小于 brand.wcm-api-concurrency，否则限流被池大小截断
     */
    private Integer poolMaxTotal = 10;

    /**
     * 单路由（wcm-api 同一主机）连接数上限，不得小于 brand.wcm-api-concurrency
     */
    private Integer poolMaxPerRoute = 10;
}
