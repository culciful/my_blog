package com.culciful;

import com.culciful.utils.ArticleCover;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 一次性脚本：给已有文章（含草稿）回填 blog.cover_url（正文第一张图）。
 * 新增这一列前发布的文章 cover_url 是 NULL，主页列表不会显示缩略图，跑一次这个补上。
 *
 * <p>跑法：设 DB_URL / DB_USERNAME / DB_PASSWORD 环境变量，或改下面的兜底值，然后 run main()。</p>
 */
public class ArticleCoverBackfill {

    public static void main(String[] args) throws Exception {
        String url = getenv("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/culciful_blog?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
        String user = getenv("DB_USERNAME", "root");
        String password = getenv("DB_PASSWORD", "root");

        int updated = 0;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement select = conn.createStatement();
             ResultSet rs = select.executeQuery(
                     "SELECT b.id, b.cover_url, tb.body "
                             + "FROM blog b JOIN text_body tb ON b.content_text_id = tb.id "
                             + "WHERE b.is_deleted = 0");
             PreparedStatement update = conn.prepareStatement(
                     "UPDATE blog SET cover_url = ? WHERE id = ?")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String recomputed = ArticleCover.firstImage(rs.getString("body"));
                String existing = rs.getString("cover_url");
                if (!java.util.Objects.equals(recomputed, existing)) {
                    update.setString(1, recomputed);
                    update.setLong(2, id);
                    update.executeUpdate();
                    updated++;
                    System.out.println("blog " + id + " -> " + recomputed);
                }
            }
        }
        System.out.println("done, " + updated + " rows updated");
    }

    private static String getenv(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
