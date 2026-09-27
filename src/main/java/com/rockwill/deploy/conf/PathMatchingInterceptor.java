package com.rockwill.deploy.conf;

import com.rockwill.deploy.utils.PathMatchUtils;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PathMatchingInterceptor implements HandlerInterceptor {

    @Autowired
    private BrandConfig brandConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getServletPath();

        if (path.startsWith("/page")){
            path = path.substring(5);
        }
        String host = resolveHost(request.getHeader("Host"));
        request.setAttribute("resolvedHost", host);
        PathMatchUtils.MatchResult matchResult = PathMatchUtils.matchResult(path, host);
        if (matchResult.getPatternType() == null
                || matchResult.getIllegal()) {
            log.error("illegal request url : {}", path);
            response.sendRedirect("/");
            return false;
        }
        request.setAttribute("patternType", matchResult.getPatternType().name());
        request.setAttribute("forwardTarget", matchResult.getForwardTarget());
        //处理非法请求
        List<String> menuName = UriComponentsBuilder.fromUriString(matchResult.getForwardTarget())
                .build()
                .getQueryParams().get("name");
        if (menuName != null && !menuName.isEmpty() && !menuName.get(0).startsWith("search")) {
            boolean isNormalMenu = SiteMenuUtils.getMenuPages(host).stream().map(SitePage::getPageName).collect(Collectors.toList()).contains(menuName.get(0));
            if (!isNormalMenu && !menuName.get(0).startsWith("home")) {
                log.error("illegal request:{}", path);
                response.sendRedirect("/");
                return false;
            }
        }
        request.setAttribute("X-Original-URI", request.getRequestURI());
        return true;
    }

    /**
     * 将请求 Host 头归一到配置域名集合（brand.domain + domain-list）内：
     * 去掉端口、忽略大小写后对齐配置原值，保证 SiteMenuUtils 等按域名键控的缓存命中；
     * Host 缺失或不在配置内时回退主域名。
     */
    private String resolveHost(String host) {
        String mainDomain = brandConfig.getDomain();
        if (host == null || host.trim().isEmpty()) {
            return mainDomain;
        }
        String normalized = host.trim();
        int portIndex = normalized.indexOf(':');
        if (portIndex > 0) {
            normalized = normalized.substring(0, portIndex);
        }
        List<String> knownDomains = new ArrayList<>();
        if (mainDomain != null && !mainDomain.trim().isEmpty()) {
            knownDomains.add(mainDomain);
        }
        if (brandConfig.getDomainList() != null) {
            knownDomains.addAll(brandConfig.getDomainList());
        }
        for (String known : knownDomains) {
            if (known != null && known.equalsIgnoreCase(normalized)) {
                return known;
            }
        }
        return mainDomain;
    }

}