package com.luoxianjun.community.module.governance;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 发帖前置规则检查。
 *
 * <p>定位是「最低成本的兜底层」：只拦明确的高风险词，命中即转人工审核，
 * 不直接删内容（避免误伤），也不等于真实性认证。后续可替换为可配置词库、
 * 模型判定或第三方审核服务，调用方只依赖 {@link #firstHit(String)} 的返回值。</p>
 */
@Component
public class ContentRuleChecker {

    private static final List<String> HIGH_RISK_WORDS = List.of(
            "赌博", "诈骗", "刷单", "代开发票", "买卖账号", "违规广告", "私彩", "套现");

    /** 返回首个命中的规则词；未命中返回 null */
    public String firstHit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : HIGH_RISK_WORDS) {
            if (lower.contains(word)) {
                return word;
            }
        }
        return null;
    }
}
