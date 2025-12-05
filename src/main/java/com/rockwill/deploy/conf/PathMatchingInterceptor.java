package com.rockwill.deploy.conf;

import com.rockwill.deploy.utils.PathMatchUtils;
import com.rockwill.deploy.utils.PathPatternType;
import com.rockwill.deploy.utils.SiteMenuUtils;
import com.rockwill.deploy.vo.SitePage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PathMatchingInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getServletPath();

        PathMatchUtils.MatchResult matchResult = PathMatchUtils.matchResult(path);
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
            boolean isNormalMenu = SiteMenuUtils.getMenuPages().stream().map(SitePage::getPageName).collect(Collectors.toList()).contains(menuName.get(0));
            if (!isNormalMenu) {
                log.error("illegal request:{}", path);
                response.sendRedirect("/");
                return false;
            }
        }
        request.setAttribute("X-Original-URI", request.getRequestURI());
        return true;
    }

}