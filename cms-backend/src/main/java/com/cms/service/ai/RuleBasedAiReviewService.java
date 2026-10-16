package com.cms.service.ai;

import com.cms.service.ai.AiReviewResult;
import com.cms.service.ai.AiReviewService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 规则版评论机审（第四阶段实现，文档 7.5）。
 *
 * 启发式规则：广告特征 / 辱骂词 / 违规词 / 重复刷屏检测。
 * 置信度按命中类别数加权；第七阶段由 Spring AI 真实模型替换本实现（接口不变）。
 */
@Service("ruleBasedAiReviewService")
public class RuleBasedAiReviewService implements AiReviewService {

    private static final String MODEL = "rule-based-v1";

    /** 模型标识（供上层标注降级来源） */
    public String modelName() {
        return MODEL;
    }

    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern LINK = Pattern.compile("https?://|www\\.|\\.[a-z]{2,}/");
    private static final Pattern QQ = Pattern.compile("[Qq][Qq]?\\s*\\d{5,}");
    private static final Pattern WECHAT = Pattern.compile("(微信|wx|vx|v信|威信)\\s*[：:+]?\\s*[\\w\\d-]{3,}");

    private static final String[] AD_WORDS = {
            "优惠", "促销", "免费领取", "加我", "联系我", "点击链接", "私信", "特价", "秒杀",
            "代购", "刷单", "兼职", "日结", "红包"
    };
    private static final String[] ABUSE_WORDS = {
            "傻", "白痴", "脑残", "垃圾", "废物", "去死", "滚蛋", "蠢货", "混蛋", "不要脸", "恶心"
    };
    /** 明确违规内容演示词（商业违规类，非政治） */
    private static final String[] SENSITIVE_WORDS = {
            "赌博", "博彩", "六合彩", "开奖", "色情", "代开发票", "办假证", "假钞", "毒品", "枪支"
    };

    private static final Pattern REPEAT = Pattern.compile("(.{2,})\\1{2,}");

    @Override
    public AiReviewResult review(String content) {
        if (content == null || content.isBlank()) {
            return AiReviewResult.builder()
                    .result("normal").score(new BigDecimal("0.08"))
                    .reason("内容为空，直接通过").model(MODEL).build();
        }
        String text = content.trim();

        // 1. 重复刷屏检测（短内容连续重复或去重后大幅缩水）
        if (REPEAT.matcher(text).find() || isRepeated(text)) {
            return AiReviewResult.builder()
                    .result("repeat").score(new BigDecimal("0.85"))
                    .reason("疑似刷屏重复内容").model(MODEL).build();
        }

        // 2. 违规特征命中统计
        List<String> hits = new ArrayList<>();
        int adHits = 0;
        if (PHONE.matcher(text).find() || LINK.matcher(text).find() || QQ.matcher(text).find() || WECHAT.matcher(text).find()) {
            adHits++;
        }
        for (String w : AD_WORDS) {
            if (text.contains(w)) {
                adHits++;
            }
        }
        int abuseHits = countHits(text, ABUSE_WORDS);
        int sensitiveHits = countHits(text, SENSITIVE_WORDS);

        // 3. 分类与置信度
        if (adHits > 0 || abuseHits > 0 || sensitiveHits > 0) {
            String result;
            int total;
            if (abuseHits >= sensitiveHits && abuseHits >= adHits && abuseHits > 0) {
                result = "abuse";
                total = abuseHits;
            } else if (sensitiveHits > 0 && sensitiveHits >= adHits) {
                result = "sensitive";
                total = sensitiveHits;
            } else {
                result = "ad";
                total = adHits;
            }
            // 置信度 = 0.6 + 0.12 * 命中数（多类别叠加），封顶 0.99
            int totalHits = abuseHits + sensitiveHits + adHits;
            double score = Math.min(0.99, 0.6 + 0.12 * totalHits);
            String reason = buildReason(result, total);
            return AiReviewResult.builder()
                    .result(result)
                    .score(BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP))
                    .reason(reason).model(MODEL).build();
        }

        // 4. 正常评论：score 统一为"违规置信度"，未命中违规特征 → 低分（与 Spring AI prompt 语义一致）
        return AiReviewResult.builder()
                .result("normal")
                .score(new BigDecimal("0.08"))
                .reason("未命中违规特征，自动通过").model(MODEL).build();
    }

    private int countHits(String text, String[] words) {
        int n = 0;
        for (String w : words) {
            if (text.contains(w)) {
                n++;
            }
        }
        return n;
    }

    /** 去重后内容明显缩水（>20 字且去重后不足 60%）判定为重复 */
    private boolean isRepeated(String text) {
        if (text.length() <= 20) {
            return false;
        }
        Set<Character> chars = new HashSet<>();
        for (char c : text.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                chars.add(c);
            }
        }
        return chars.size() < text.length() * 0.6;
    }

    private String buildReason(String result, int hits) {
        return switch (result) {
            case "ad" -> "检测到广告特征（联系方式/链接/营销词 ×" + hits + "）";
            case "abuse" -> "检测到辱骂词汇 ×" + hits;
            case "sensitive" -> "检测到违规敏感词 ×" + hits;
            default -> "疑似违规";
        };
    }
}
