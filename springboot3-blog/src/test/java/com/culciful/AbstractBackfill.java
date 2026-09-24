package com.culciful;

import com.culciful.utils.ArticleAbstract;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 一次性脚本：用新的摘要逻辑（剥 markdown + 词边界截断）重算所有「非作者自填」文章的 blog.abstract。
 * 改逻辑后老文章的 abstract 不会自动更新。作者自填的（abstract_custom=1）跳过。
 *
 * <p>跑法：设 DB_URL / DB_USERNAME / DB_PASSWORD 环境变量，或改下面的兜底值，然后 run main()。</p>
 */
public class AbstractBackfill {

    public static void main(String[] args) throws Exception {
        String url = getenv("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/culciful_blog?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
        String user = getenv("DB_USERNAME", "root");
        String password = getenv("DB_PASSWORD", "root");

        int updated = 0;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement select = conn.createStatement();
             ResultSet rs = select.executeQuery(
                     "SELECT b.id, b.`abstract`, tb.body "
                             + "FROM blog b JOIN text_body tb ON b.content_text_id = tb.id "
                             + "WHERE b.is_deleted = 0 AND b.is_custom_abstract = 0");
             PreparedStatement update = conn.prepareStatement(
                     "UPDATE blog SET `abstract` = ? WHERE id = ?")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String recomputed = ArticleAbstract.auto(rs.getString("body"));
                if (!recomputed.equals(rs.getString("abstract"))) {
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
