package com.rockwill.deploy.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class RestTemplateHeaderInterceptor implements ClientHttpRequestInterceptor {

    @Autowired
    BrandConfig brandConfig;
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
//        request.getHeaders().add("Deploy-Domain", brandConfig.getDomain());
        String originalUri = getOriginalUriFromCurrentRequest();
        if (originalUri != null && !originalUri.isEmpty()) {
            request.getHeaders().add("X-Original-URI", originalUri);
        }
        return execution.execute(request, body);
    }


    private String getOriginalUriFromCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            HttpServletRequest currentRequest = attributes.getRequest();
            String originalUri = (String) currentRequest.getAttribute("X-Original-URI");
            if (originalUri != null) {
                return originalUri;
            }
            originalUri = currentRequest.getHeader("X-Original-URI");
            if (originalUri != null) {
                return originalUri;
            }
            return currentRequest.getRequestURI();
        } catch (Exception e) {
            return null;
        }
    }
}