package com.luoxianjun.community.module.content.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 搜索词归一化：把用户输入切成若干「词」，供多字段包含匹配使用。
 *
 * <p>只保留 Unicode 字母 / 数字（含汉字），丢弃标点与 LIKE 通配符。
 * 参数绑定已经防住 SQL 注入，这里额外清洗是为了避免用户输入 "%" 时
 * 把整张表当成命中结果，也避免超长查询拖垮数据库。</p>
 */
public final class PostKeyword {

    public static final int MAX_WORDS = 5;
    public static final int MAX_WORD_LENGTH = 40;
    private static final int MAX_INPUT_LENGTH = 200;

    private PostKeyword() {
    }

    public static List<String> split(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String input = keyword.trim();
        if (input.length() > MAX_INPUT_LENGTH) {
            input = input.substring(0, MAX_INPUT_LENGTH);
        }
        // 空格、逗号、顿号、分号、竖线、斜杠、加号都当作「词与词的分隔」
        String[] raw = input.split("[\\s,，、;；|/\\\\+]+");
        LinkedHashSet<String> words = new LinkedHashSet<>();
        for (String part : raw) {
            String word = part.replaceAll("[^\\p{L}\\p{N}]", "");
            if (word.isEmpty()) {
                continue;
            }
            if (word.length() > MAX_WORD_LENGTH) {
                word = word.substring(0, MAX_WORD_LENGTH);
            }
            words.add(word);
            if (words.size() >= MAX_WORDS) {
                break;
            }
        }
        return new ArrayList<>(words);
    }
}
