package com.cms.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * 富文本 XSS 清洗工具（文档 7.8：基于 JSoup 白名单）。
 * 仅保留安全标签，移除事件属性与 javascript: 协议。
 */
public final class SanitizeUtil {

    private SanitizeUtil() {
    }

    /** 富文本白名单：基础标签 + 标题/列表/引用/代码/表格（文档 7.8 清单） */
    private static final Safelist RICH_TEXT_SAFELIST = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            .addTags("code", "pre")
            .addTags("div", "span")
            .addTags("table", "thead", "tbody", "tr", "th", "td", "caption")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("code", "class")
            .addAttributes("pre", "class")
            .addAttributes("div", "class")
            .addAttributes("span", "class")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto");

    /**
     * 清洗富文本 HTML（去除脚本、事件属性、非法协议；保留白名单标签）
     */
    public static String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(false);
        return Jsoup.clean(html, "", RICH_TEXT_SAFELIST, settings);
    }

    /**
     * 提取纯文本（用于自动生成摘要）
     */
    public static String plainText(String html, int maxLen) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(html).text().trim();
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }
}
