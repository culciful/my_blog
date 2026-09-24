package com.culciful.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleAbstractTest {

    @Test
    void fromAuthorStripsMarkdownNoEllipsis() {
        assertEquals("手写的摘要", ArticleAbstract.fromAuthor("  **手写的摘要** "));
    }

    @Test
    void autoMoreMarkerCutsWithEllipsis() {
        assertEquals("开头一段。…", ArticleAbstract.auto("开头一段。<!-- more -->\n后面的全文很长很长"));
    }

    @Test
    void autoShortContentNoEllipsis() {
        assertEquals("s", ArticleAbstract.auto("s"));
        assertEquals("很短的一句话。", ArticleAbstract.auto("很短的一句话。"));
    }

    @Test
    void autoStripsMarkdownMarkers() {
        String md = "## 引言\n这是 **加粗** 和 *斜体*，还有 [链接](http://x) 和 `code`。\n"
                + "![图片](/uploads/a.png)\n> 引用\n- 列表项";
        String out = ArticleAbstract.auto(md);
        assertFalse(out.contains("#"));
        assertFalse(out.contains("*"));
        assertFalse(out.contains("]("));
        assertFalse(out.contains("!["));
        assertFalse(out.contains("`"));
        assertTrue(out.contains("加粗"));
        assertTrue(out.contains("链接"));
        assertTrue(out.contains("引言"));
    }

    @Test
    void keepsUnderscoreInsideTokens() {
        assertEquals(":kissing_heart: some_var", ArticleAbstract.normalize(":kissing_heart: some_var"));
    }

    @Test
    void autoWordBoundaryTruncationWithEllipsis() {
        String longText = "word ".repeat(60).trim();
        String out = ArticleAbstract.auto(longText);
        assertTrue(out.endsWith("…"));
        assertTrue(out.length() <= ArticleAbstract.AUTO_MAX + 1);
        assertFalse(out.substring(0, out.length() - 1).endsWith("wor"));
    }

    @Test
    void collapsesWhitespaceAndFullWidthSpace() {
        assertEquals("a b c", ArticleAbstract.normalize("a\n\n b\t　c"));
    }
}
