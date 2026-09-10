package com.culciful.utils;

/**
 * 生成文章列表用的摘要（存进 blog.abstract）。
 * 作者填了就用作者的（{@link #fromAuthor}），没填就从正文自动生成（{@link #auto}）。
 * 两条都会先剥掉 markdown 标记、压缩空白。
 */
public final class ArticleAbstract {

    /** 自动截取的字符上限 */
    static final int AUTO_MAX = 120;
    /** 作者自填 / more 段的字符上限 */
    static final int MANUAL_MAX = 200;

    private static final String MORE_MARKER = "<!-- more -->";

    private ArticleAbstract() {
    }

    /** 作者自填：剥 markdown + 截 200，当作一段完整的话，不加省略号 */
    public static String fromAuthor(String authorText) {
        return truncate(normalize(authorText), MANUAL_MAX, false);
    }

    /** 无作者摘要时从正文生成：优先 {@code <!-- more -->} 之前，否则整篇自动截取 */
    public static String auto(String content) {
        String md = content == null ? "" : content;
        int moreAt = md.indexOf(MORE_MARKER);
        if (moreAt > 0) {
            // <!-- more -->：后面必然还有正文，补省略号
            return withEllipsis(truncate(normalize(md.substring(0, moreAt)), MANUAL_MAX, false));
        }
        // 整篇自动截取：只有真被截断时 truncate() 才补省略号；正文很短则原样
        return truncate(normalize(md), AUTO_MAX, true);
    }

    private static String withEllipsis(String s) {
        return s.isEmpty() || s.endsWith("…") ? s : s + "…";
    }

    /** 剥 markdown + 压空白 */
    static String normalize(String md) {
        if (md == null) {
            return "";
        }
        String s = md
                // 代码块 / 公式块整段丢弃
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("(?s)~~~.*?~~~", " ")
                .replaceAll("(?s)\\$\\$.*?\\$\\$", " ")
                // 图片丢弃、链接保留文字
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", "")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                // 标题 / 引用 / 列表符 / 分隔线（行首）
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s+", "")
                .replaceAll("(?m)^\\s{0,3}>\\s?", "")
                .replaceAll("(?m)^\\s{0,3}(?:[-*+]|\\d+\\.)\\s+", "")
                .replaceAll("(?m)^\\s{0,3}(?:[-*_]\\s*){3,}$", " ")
                // 表格竖线、行内代码
                .replaceAll("\\|", " ")
                .replaceAll("`([^`]*)`", "$1")
                // 成对标记：** ~~ 总是去；__ 仅在非词内时去
                .replaceAll("\\*\\*|~~|(?<![A-Za-z0-9])__(?![A-Za-z0-9])", "")
                // 单个 *：两侧都不是字母数字才当强调符去掉（保留 a*b、2*3 里的 *，但 2 * 3 会被去）
                .replaceAll("(?<![A-Za-z0-9])\\*(?![A-Za-z0-9])", "")
                // 单个 _：只在贴着空白 / 首尾时去（保留 :kissing_heart:、some_var；_斜体_ 的下划线留着，摘要里无所谓）
                .replaceAll("(?<=^|\\s)_(?=\\S)|(?<=\\S)_(?=\\s|$)", "")
                // 残留 html 标签
                .replaceAll("<[^>]+>", "");
        return s.replaceAll("[\\s\\u3000]+", " ").trim();
    }

    /** 超过 max 时按最后一个空格断开（断点太靠前就硬截），可选补「…」 */
    static String truncate(String text, int max, boolean appendEllipsis) {
        if (text.length() <= max) {
            return text;
        }
        int space = text.lastIndexOf(' ', max);
        String head = space > max / 2 ? text.substring(0, space) : text.substring(0, max);
        return appendEllipsis ? head + "…" : head;
    }
}
