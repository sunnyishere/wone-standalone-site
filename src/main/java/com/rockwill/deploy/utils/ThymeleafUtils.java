package com.rockwill.deploy.utils;

/**
 * 字符串处理工具类
 */
public class ThymeleafUtils {
    
    /**
     * 增强的字符串规范化方法
     * 处理特殊字符：括号、点、波浪线等替换为下划线
     */
    public static String normalizeString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        // 初始处理：转小写，替换斜杠和空格
        String processed = input.toLowerCase().trim();
        
        // 处理特殊字符：括号、点、波浪线等
        processed = processed
                .replaceAll("[\\s_]+", "-")  // 替换空格和下划线为连字符
                .replaceAll("[^a-z0-9-]", "") // 移除非字母数字和连字符的字符
                .replaceAll("-+", "-")        // 将多个连字符替换为单个
                .replaceAll("^-|-$", "");     // 移除开头和结尾的连字符
        
        return processed;
    }
    
    /**
     * 简化版本：只处理特定特殊字符
     */
    public String replaceSpecialChars(String input) {
        if (input == null) return null;
        return input.replaceAll("[.()\\[\\]{}~!@#$%^&*+=|\\\\:;'\",<>?]", "-");
    }
}