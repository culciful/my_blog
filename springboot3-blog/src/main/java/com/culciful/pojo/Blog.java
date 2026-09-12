package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Article table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("blog")
public class Blog {

    /**
     * Snowflake ID / article ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Author user ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Title
     */
    @TableField("title")
    private String title;

    /**
     * 列表展示摘要（对外 JSON key = "abstract"；Java 里 abstract 是关键字所以字段叫 abstractText）。
     * 作者自填时 = 其原文（剥 markdown + 截 200）；未自填时从正文自动生成。
     */
    @TableField("abstract")
    private String abstractText;

    /**
     * abstract 是否作者自填。true：编辑页回填输入框、改正文不重算；false：从正文自动生成
     */
    @TableField("is_custom_abstract")
    private Boolean isCustomAbstract;

    /**
     * 正文第一张图片 URL（{@link com.culciful.utils.ArticleCover}），列表缩略图用；没图为 null。
     * 每次发布/编辑正文都会重算，作者不能自定义。
     * 编辑时可能把图删没了（值变回 null），MP 默认 NOT_NULL 更新策略会跳过 null 字段、导致旧封
     * 面残留——这个字段的 update 必须无条件带上，哪怕是 null。本来用
     * {@code @TableField(updateStrategy = FieldStrategy.IGNORED)} 解决，但这个注解形式在当前
     * 工具链组合下会产出损坏的 class 文件（见 {@link com.culciful.controller.ArticleController
     * #forceCoverUrl}），改成在 controller 里显式二次 update 绕开。
     */
    @TableField(value = "cover_url")
    private String coverUrl;

    /**
     * 发布状态：{@code draft} 草稿（仅作者可见，不进公开列表/详情、不计入 article_count）/
     * {@code published} 已发布。
     */
    @TableField("status")
    private String status;

    /**
     * Content text_body.id
     */
    @TableField("content_text_id")
    private Long contentTextId;

    /**
     * View count
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * Comment count
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * Package ID
     */
    @TableField("package_id")
    private Long packageId;

    /**
     * Created time (UTC)
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * Updated time (UTC)
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 最后互动时间 (UTC)：发布 / 编辑正文 / 收到新评论都会刷新。
     * 主页 feed 按它倒序（编辑或被评论会把文章顶上去）；作者维度的列表仍按 created_at。
     */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    /**
     * Deleted flag
     */
    @TableField("is_deleted")
    private Boolean isDeleted;
}