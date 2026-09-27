package com.rockwill.deploy.utils;

import java.text.Normalizer;

import net.sourceforge.pinyin4j.PinyinHelper;

/**
 * 字符串处理工具类
 */
public class ThymeleafUtils {

    /**
     * 将标题/名称规范化为 URL slug。
     * 口径与后端一致：
     * 转小写、NFKD 去除音标、中文转拼音、保留数字之间的点（如 6.0）、
     * 折叠空格及特殊字符为连字符、移除独立的数字词段（如 top-10-products → top-products）。
     *
     * @param input 待规范化的原始标题或名称
     * @return 规范化后的 slug；入参为 null 或空白时原样返回
     */
    public static String normalizeString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        input = Normalizer.normalize(input.trim(), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        StringBuilder sb = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '.' && i > 0 && i < chars.length - 1
                    && Character.isDigit(chars[i - 1]) && Character.isDigit(chars[i + 1])) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') {
                    sb.append(c);
                }
            } else if (c == ' ' || c == '/' || c == '-' || !Character.isLetterOrDigit(c)) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
                    sb.append('-');
                }
            } else if (c >= '\u4e00' && c <= '\u9fa5') {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    String pinyin = pinyinArray[0].toLowerCase().replaceAll("[^a-z]", "");
                    sb.append(pinyin).append("-");
                }
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        String slug = sb.toString();
        slug = slug.replaceAll("[^a-zA-Z0-9\\s-.]", "");
        slug = slug.replaceAll("[\\s\\u00A0#]+", "-");
        slug = slug.replaceAll("-\\d+-", "-");
        slug = slug.replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");
        return slug;
    }

    /**
     * 简化版本：只处理特定特殊字符
     */
    public String replaceSpecialChars(String input) {
        if (input == null) return null;
        return input.replaceAll("[.()\\[\\]{}~!@#$%^&*+=|\\\\:;'\",<>?]", "-");
    }
}
