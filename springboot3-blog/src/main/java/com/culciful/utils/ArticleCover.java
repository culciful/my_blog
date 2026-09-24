package com.culciful.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从正文里挑出第一张图片的 URL（存进 blog.cover_url），主页列表缩略图用。
 * 只认标准 markdown 图片语法 {@code ![alt](url)}（编辑器上传图片就是插的这个），
 * 代码块 / 公式块里出现的图片语法不算（跳过再找）。
 */
public final class ArticleCover {

    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\(\\s*(\\S+?)(?:\\s+\"[^\"]*\")?\\s*\\)");

    private ArticleCover() {
    }

    /** 正文里第一张图的 URL；没有图返回 null */
    public static String firstImage(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        // 代码块 / 公式块整段丢弃，避免示例代码里的图片语法被当真
        String stripped = content
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("(?s)~~~.*?~~~", " ")
                .replaceAll("(?s)\\$\\$.*?\\$\\$", " ");
        Matcher m = IMAGE.matcher(stripped);
        if (!m.find()) {
            return null;
        }
        String url = m.group(1).trim();
        return url.isEmpty() ? null : url;
    }
}
