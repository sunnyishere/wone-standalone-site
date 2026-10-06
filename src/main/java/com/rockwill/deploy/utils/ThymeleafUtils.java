package com.rockwill.deploy.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字符串处理工具类（Thymeleaf #normalizer）
 */
public class ThymeleafUtils {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "for", "with", "of", "in", "on", "at", "to", "by", "from", "is"
    ));
    private static final int MAX_CHARS = 96;
    private static final int MAX_WORDS = 8;

    /**
     * 增强的字符串规范化方法
     * 处理特殊字符：括号、点、波浪线等替换为下划线
     * <p>仅用于分类名等非详情用途；详情 slug 请用 {@link #resolveSlugUrl}。
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

    /**
     * 拼详情 URL slug 段：库值非空则原样 trim（禁止再 normalize）；空则 generateSmartSlug(title)。
     * 语义对齐 knowledge StringUtils.resolveSlugUrl。
     */
    public static String resolveSlugUrl(String slugFromDb, String title) {
        if (slugFromDb != null && !slugFromDb.trim().isEmpty()) {
            return slugFromDb.trim();
        }
        String slug = generateSmartSlug(title);
        return (slug == null || slug.isEmpty()) ? "item" : slug;
    }

    /**
     * 读侧灌详情 slug：库字段优先；库空则保留已灌 slugUrl；仍空才按 title 现算。
     */
    public static String coalesceDetailSlugUrl(String existingSlugUrl, String slugFromDb, String title) {
        if (slugFromDb != null && !slugFromDb.trim().isEmpty()) {
            return slugFromDb.trim();
        }
        if (existingSlugUrl != null && !existingSlugUrl.trim().isEmpty()) {
            return existingSlugUrl.trim();
        }
        return resolveSlugUrl(null, title);
    }

    /**
     * 与 knowledge StringUtils.generateSmartSlug 对齐：停用词、去纯数字词、≤8 word、≤96、中文转拼音。
     */
    public static String generateSmartSlug(String entitle) {
        if (entitle == null || entitle.trim().isEmpty()) {
            return "";
        }

        boolean hasChinese = countChineseChars(entitle) > 0;
        String cleaned;
        if (hasChinese) {
            cleaned = toPinyin(entitle);
        } else {
            cleaned = entitle.replace('/', ' ').replaceAll("[^a-zA-Z0-9\\s-.]", "").toLowerCase();
        }

        String[] words = cleaned.split("[\\s-]+");
        List<String> filteredWords;
        if (hasChinese) {
            filteredWords = Arrays.stream(words)
                    .filter(word -> !word.isEmpty() && !NumberUtils.isDigits(word))
                    .collect(Collectors.toList());
        } else {
            filteredWords = Arrays.stream(words)
                    .filter(word -> !word.isEmpty() && !STOP_WORDS.contains(word) && !NumberUtils.isDigits(word))
                    .collect(Collectors.toList());
        }

        StringBuilder slugBuilder = new StringBuilder();
        int wordCount = 0;

        for (String word : filteredWords) {
            if (wordCount >= MAX_WORDS) {
                break;
            }
            int nextLength = slugBuilder.length() + (slugBuilder.length() > 0 ? 1 : 0) + word.length();
            if (nextLength > MAX_CHARS) {
                break;
            }
            if (slugBuilder.length() > 0) {
                slugBuilder.append("-");
            }
            slugBuilder.append(word);
            wordCount++;
        }

        return slugBuilder.toString();
    }

    private static int countChineseChars(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                count++;
            }
        }
        return count;
    }

    private static String toPinyin(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == ' ' || c == '-' || c == '.' || c == '/') {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            } else if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    String pinyin = pinyinArray[0].toLowerCase().replaceAll("[^a-z]", "");
                    sb.append(pinyin).append(' ');
                }
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString().trim();
    }
}
