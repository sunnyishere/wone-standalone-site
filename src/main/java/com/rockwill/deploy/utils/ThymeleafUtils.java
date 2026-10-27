package com.rockwill.deploy.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * 拼分类 URL slug 段：库值非空则原样 trim；空则 {@link #normalizeString}(name)。
     * 勿与详情 {@link #resolveSlugUrl}（smartSlug 兜底）混用。
     */
    public static String resolveCategorySlugUrl(String slugFromDb, String name) {
        if (slugFromDb != null && !slugFromDb.trim().isEmpty()) {
            return slugFromDb.trim();
        }
        String slug = normalizeString(name);
        return slug == null ? "" : slug;
    }

    /**
     * 从 Map / 带 getSlugUrl 的对象取分类段。Map 缺键时不抛 EL1008E（静态化 JSON 常省略 null）。
     */
    public static String resolveCategorySlugUrl(Object source, Object name) {
        return resolveCategorySlugUrl(readMapOrBeanString(source, "slugUrl", "getSlugUrl"),
                name == null ? null : name.toString());
    }

    /**
     * 从 Map / 带 getter 的对象取详情段；Map 缺键安全。
     */
    public static String resolveSlugUrl(Object source, Object title) {
        return resolveSlugUrl(readMapOrBeanString(source, "slugUrl", "getSlugUrl"),
                title == null ? null : title.toString());
    }

    private static String readMapOrBeanString(Object source, String mapKey, String getterName) {
        if (source == null) {
            return null;
        }
        if (source instanceof Map) {
            Object value = ((Map<?, ?>) source).get(mapKey);
            return value == null ? null : value.toString();
        }
        if (source instanceof CharSequence) {
            return source.toString();
        }
        try {
            java.lang.reflect.Method getter = source.getClass().getMethod(getterName);
            Object value = getter.invoke(source);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /** 子分类卡片缺 slugUrl 时，从同页 association 按 id 回填。 */
    @SuppressWarnings("unchecked")
    public static void fillChildCategoryCardSlugUrls(Map<String, Object> model) {
        if (model == null) {
            return;
        }
        Object cardsObj = model.get("childCategoryCards");
        Object assocObj = model.get("association");
        if (!(cardsObj instanceof List) || !(assocObj instanceof List)) {
            return;
        }
        List<?> cards = (List<?>) cardsObj;
        if (cards.isEmpty()) {
            return;
        }
        Map<String, String> idToSlug = new HashMap<>();
        collectAssociationSlugUrls((List<?>) assocObj, idToSlug);
        if (idToSlug.isEmpty()) {
            return;
        }
        for (Object card : cards) {
            if (!(card instanceof Map)) {
                continue;
            }
            Map<String, Object> cardMap = (Map<String, Object>) card;
            Object existing = cardMap.get("slugUrl");
            if (existing != null && !existing.toString().trim().isEmpty()) {
                continue;
            }
            Object id = cardMap.get("id");
            if (id == null) {
                continue;
            }
            String slug = idToSlug.get(id.toString());
            if (slug != null && !slug.isEmpty()) {
                cardMap.put("slugUrl", slug);
            }
        }
    }

    private static void collectAssociationSlugUrls(List<?> nodes, Map<String, String> out) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (Object node : nodes) {
            if (!(node instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) node;
            Object id = map.get("id");
            Object slug = map.get("slugUrl");
            if (id != null && slug != null && !slug.toString().trim().isEmpty()) {
                out.put(id.toString(), slug.toString().trim());
            }
            Object children = map.get("children");
            if (children instanceof List) {
                collectAssociationSlugUrls((List<?>) children, out);
            }
        }
    }

    /**
     * 拼详情 URL slug 段：库值非空则原样 trim（禁止再 normalize）；空则 generateSmartSlug(title)。
     * 语义对齐 knowledge StringUtils.resolveSlugUrl。
     */
    public static String resolveSlugUrl(String slugFromDb, String title) {
        if (slugFromDb != null && !slugFromDb.trim().isEmpty()) {
            return slugFromDb.trim().replace("|-", "").replace("：", "")
                    .replace(":", "");
        }
        String slug = generateSmartSlug(title);
        return slug.isEmpty() ? "item" : slug;
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
