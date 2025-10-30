package com.rockwill.deploy.vo;

import lombok.Data;

import java.util.Map;

@Data
public class DomainHtmlVo {
    private String htmlContent;
    private Integer totalPages;
    private Map<String, Object> modelMap;

}
