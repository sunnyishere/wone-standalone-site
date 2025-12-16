package com.rockwill.deploy.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockwill.deploy.utils.AccessRecord;
import com.rockwill.deploy.vo.SecurityReq;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ApiSignatureInterceptor implements HandlerInterceptor {

    @Autowired
    BrandConfig brandConfig;

    @Autowired
    private ObjectMapper objectMapper;
    private final Map<String, AccessRecord> appAccessRecords = new ConcurrentHashMap<>();

    private static final long ONE_HOUR = 60 * 60 * 1000L;
    private static final long ONE_DAY = 24 * ONE_HOUR;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ContentCachingRequestWrapper requestWrapper = (ContentCachingRequestWrapper) request;

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            buildErrorResponse(response, "Method Not Allowed");
            return false;
        }
        SecurityReq securityReq = new SecurityReq();
        Map<String, String[]> parameterMap = requestWrapper.getParameterMap();
        for (String key : parameterMap.keySet()) {
            switch ( key){
                case "appId":
                    securityReq.setAppId(parameterMap.get(key)[0]);
                    break;
                case "timestamp":
                    securityReq.setTimestamp(Long.parseLong(parameterMap.get(key)[0]));
                    break;
                    case "nonce":
                    securityReq.setNonce(parameterMap.get(key)[0]);
                    break;
                    case "signature":
                    securityReq.setSignature(parameterMap.get(key)[0]);
                    break;
                    case "data":
                    securityReq.setData(parameterMap.get(key)[0]);
            }
        }

        if (ObjectUtils.isEmpty(securityReq.getNonce())
                || ObjectUtils.isEmpty(securityReq.getSignature())) {
            buildErrorResponse(response, "Request body is empty or invalid");
            return false;
        }

        if (!securityReq.hasEssentialParams()) {
            buildErrorResponse(response, "Missing essential parameters");
            return false;
        }

        String secret = brandConfig.getSecret();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appId", securityReq.getAppId());
        paramMap.put("timestamp", securityReq.getTimestamp().toString());
        paramMap.put("nonce", securityReq.getNonce());
        paramMap.put("data", securityReq.getData().toString());
        String paramString = paramMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        String stringToSign = paramString + "&secret=" + secret;
        String signature = DigestUtils.md5DigestAsHex(stringToSign.getBytes()).toUpperCase();
        if (!signature.equals(securityReq.getSignature())) {
            buildErrorResponse(response, "Invalid signature");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - securityReq.getTimestamp());
        long expireTime = 5 * 60 * 1000; // 5分钟
        if (timeDiff > expireTime) {
            buildErrorResponse(response, "Request expired");
            return false;
        }

        AccessRecord record = appAccessRecords.computeIfAbsent(securityReq.getAppId(), k -> new AccessRecord());
        // 1. 检查1小时内次数是否超过1次
        int countLastHour = record.getCountInWindow(ONE_HOUR);
        if (countLastHour >= 1) {
            buildErrorResponse(response, "Frequency limit exceeded: maximum 1 request per hour");
            return false;
        }

        // 2. 检查24小时内次数是否超过5次
        int countLastDay = record.getCountInWindow(ONE_DAY);
        if (countLastDay >= 5) {
            buildErrorResponse(response, "Frequency limit exceeded: maximum 5 requests per day");
            return false;
        }
        record.addRecord();

        return true;
    }

    private void buildErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": 403, \"message\": \"" + message + "\"}");
    }
}