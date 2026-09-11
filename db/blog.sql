DROP DATABASE IF EXISTS `culciful_blog`;
CREATE DATABASE `culciful_blog` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `culciful_blog`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `blog_tag_relation`;
DROP TABLE IF EXISTS `blog_tag`;
DROP TABLE IF EXISTS `email_verification_code`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `blog_comment`;
DROP TABLE IF EXISTS `blog`;
DROP TABLE IF EXISTS `user_follow`;
DROP TABLE IF EXISTS `user_package`;
DROP TABLE IF EXISTS `text_body`;
DROP TABLE IF EXISTS `file_asset`;
DROP TABLE IF EXISTS `user_info`;

CREATE TABLE `user_info` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `email` VARCHAR(64) NOT NULL COMMENT '用户邮箱',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '用户密码哈希',
  `avatar_asset_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '头像资源ID',
  `article_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文章数缓存',
  `following_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注数缓存',
  `follower_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '粉丝数缓存',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间(UTC)',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `deleted_token` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除版本号: 活跃=0, 删除=唯一值',
  `token_version` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'JWT 版本号: 改密/改邮箱/全设备登出时 +1, 旧 token 失效',
  PRIMARY KEY (`id`),
  KEY `idx_user_avatar_asset` (`avatar_asset_id`),
  KEY `idx_user_active` (`is_deleted`, `deleted_token`),
  UNIQUE KEY `uk_user_email_deleted_token` (`email`, `deleted_token`),
  UNIQUE KEY `uk_user_username_deleted_token` (`username`, `deleted_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息表';

CREATE TABLE `file_asset` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '上传者用户ID',
  `asset_type` VARCHAR(32) NOT NULL COMMENT '资源类型: avatar/article_image/comment_image/attachment',
  `provider` VARCHAR(32) NOT NULL DEFAULT 'local' COMMENT '存储提供商',
  `bucket` VARCHAR(128) DEFAULT NULL COMMENT '存储桶',
  `storage_key` VARCHAR(512) NOT NULL COMMENT '对象存储Key',
  `public_url` VARCHAR(1024) NOT NULL COMMENT '公开访问URL',
  `mime_type` VARCHAR(128) NOT NULL COMMENT 'MIME类型',
  `size_bytes` BIGINT UNSIGNED NOT NULL COMMENT '文件大小(字节)',
  `content_hash` CHAR(64) DEFAULT NULL COMMENT '文件SHA-256哈希',
  `width` INT UNSIGNED DEFAULT NULL COMMENT '图片宽',
  `height` INT UNSIGNED DEFAULT NULL COMMENT '图片高',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态:1有效 0删除',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间(UTC)',
  PRIMARY KEY (`id`),
  KEY `idx_asset_owner_type` (`owner_user_id`, `asset_type`, `status`, `created_at`),
  KEY `idx_asset_hash` (`content_hash`),
  UNIQUE KEY `uk_provider_bucket_key` (`provider`, `bucket`, `storage_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资源表';

CREATE TABLE `text_body` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `body` LONGTEXT NOT NULL COMMENT '正文内容',
  `content_hash` CHAR(64) NOT NULL COMMENT '正文内容SHA-256哈希',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  PRIMARY KEY (`id`),
  KEY `idx_content_hash` (`content_hash`),
  FULLTEXT KEY `ft_text_body` (`body`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文本存储表';

CREATE TABLE `user_package` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `pack_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间(UTC)',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_package_name` (`user_id`, `pack_name`, `is_deleted`),
  KEY `idx_package_user` (`user_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户分类表';

CREATE TABLE `user_follow` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `following_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注用户ID',
  `follower_id` BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间(UTC)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow_relation` (`follower_id`, `following_id`),
  KEY `idx_following` (`following_id`),
  KEY `idx_follower` (`follower_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表';

CREATE TABLE `blog` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID/文章ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '作者用户ID',
  `title` VARCHAR(64) NOT NULL COMMENT '标题',
  `abstract` VARCHAR(255) NOT NULL COMMENT '列表展示摘要: 作者自填其原文, 否则从正文自动生成(剥markdown+截断)',
  `is_custom_abstract` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'abstract 是否作者自填(1: 编辑回填、改正文不重算)',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '正文第一张图片URL(取第一个markdown ![](url)), 列表缩略图用; 无图为NULL',
  `status` VARCHAR(16) NOT NULL DEFAULT 'published' COMMENT '发布状态: draft 草稿(仅作者可见, 不进公开列表/详情, 不计入 article_count) / published 已发布',
  `content_text_id` BIGINT UNSIGNED NOT NULL COMMENT '正文text_body.id',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
  `package_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间(UTC)',
  `last_active_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后互动时间(UTC): 发布/编辑正文/新评论刷新; 主页 feed 按此倒序, 作者维度列表仍按 created_at',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_blog_user_package` (`user_id`, `package_id`, `is_deleted`, `created_at`),
  KEY `idx_blog_user_status` (`user_id`, `status`, `is_deleted`, `updated_at`),
  KEY `idx_blog_created_at` (`created_at`),
  KEY `idx_blog_feed` (`status`, `is_deleted`, `last_active_at`),
  KEY `idx_blog_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章表';

CREATE TABLE `blog_comment` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID/评论ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
  `blog_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `author_id` BIGINT UNSIGNED NOT NULL COMMENT '文章作者用户ID',
  `is_markdown` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否Markdown',
  `content_text_id` BIGINT UNSIGNED NOT NULL COMMENT '评论内容text_body.id',
  `at_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '@用户ID',
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID',
  `root_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '根评论ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间(UTC)',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_blog` (`blog_id`, `is_deleted`, `created_at`),
  KEY `idx_comment_user` (`user_id`, `is_deleted`, `created_at`),
  KEY `idx_comment_root` (`root_id`, `is_deleted`, `created_at`),
  KEY `idx_comment_author` (`author_id`, `is_deleted`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';

CREATE TABLE `blog_tag` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `tag` VARCHAR(30) NOT NULL COMMENT '标签名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';

CREATE TABLE `blog_tag_relation` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `blog_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `tag_id` BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_tag_relation` (`blog_id`, `tag_id`),
  KEY `idx_relation_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章标签关系表';

CREATE TABLE `email_verification_code` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `email` VARCHAR(64) NOT NULL COMMENT '邮箱',
  `scene` VARCHAR(32) NOT NULL COMMENT '业务场景: register/reset/update_email',
  `code_hash` VARCHAR(128) NOT NULL COMMENT '验证码哈希',
  `expires_at` DATETIME(3) NOT NULL COMMENT '过期时间(UTC)',
  `used_at` DATETIME(3) NULL DEFAULT NULL COMMENT '使用时间(UTC)',
  `attempt_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '校验失败次数, 超过上限即作废',
  `request_ip` VARCHAR(45) DEFAULT NULL COMMENT '发起请求的客户端IP, 用于按IP限流',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  PRIMARY KEY (`id`),
  KEY `idx_email_scene_time` (`email`, `scene`, `created_at`),
  KEY `idx_ip_time` (`request_ip`, `created_at`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱验证码表';

CREATE TABLE `audit_log` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID; 登录失败等场景可能识别不到用户, 为NULL',
  `action` VARCHAR(32) NOT NULL COMMENT '事件类型: LOGIN_SUCCESS/LOGIN_FAILED/PASSWORD_CHANGED/PASSWORD_RESET/ACCOUNT_DELETED',
  `ip` VARCHAR(45) DEFAULT NULL COMMENT '来源IP',
  `detail` VARCHAR(255) DEFAULT NULL COMMENT '附加信息, 如登录失败时提交的用户名/邮箱',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间(UTC)',
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`, `created_at`),
  KEY `idx_audit_action` (`action`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全审计日志: 登录/改密/注销';

ALTER TABLE `user_package`
  ADD CONSTRAINT `fk_package_user`
  FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `user_follow`
  ADD CONSTRAINT `fk_following_user`
  FOREIGN KEY (`following_id`) REFERENCES `user_info` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_follower_user`
  FOREIGN KEY (`follower_id`) REFERENCES `user_info` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT;

ALTER TABLE `file_asset`
  ADD CONSTRAINT `fk_asset_owner_user`
  FOREIGN KEY (`owner_user_id`) REFERENCES `user_info` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `user_info`
  ADD CONSTRAINT `fk_user_avatar_asset`
  FOREIGN KEY (`avatar_asset_id`) REFERENCES `file_asset` (`id`)
  ON DELETE SET NULL ON UPDATE RESTRICT;

ALTER TABLE `blog`
  ADD CONSTRAINT `fk_blog_user`
  FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_blog_package`
  FOREIGN KEY (`package_id`) REFERENCES `user_package` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_blog_text`
  FOREIGN KEY (`content_text_id`) REFERENCES `text_body` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `blog_comment`
  ADD CONSTRAINT `fk_comment_user`
  FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_comment_blog`
  FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_comment_author`
  FOREIGN KEY (`author_id`) REFERENCES `user_info` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_comment_text`
  FOREIGN KEY (`content_text_id`) REFERENCES `text_body` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_comment_at_user`
  FOREIGN KEY (`at_user_id`) REFERENCES `user_info` (`id`)
  ON DELETE SET NULL ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_comment_parent`
  FOREIGN KEY (`parent_id`) REFERENCES `blog_comment` (`id`)
  ON DELETE SET NULL ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_comment_root`
  FOREIGN KEY (`root_id`) REFERENCES `blog_comment` (`id`)
  ON DELETE SET NULL ON UPDATE RESTRICT;

ALTER TABLE `blog_tag_relation`
  ADD CONSTRAINT `fk_relation_blog`
  FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_relation_tag`
  FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT;

SET FOREIGN_KEY_CHECKS = 1;

