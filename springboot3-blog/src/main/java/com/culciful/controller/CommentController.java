package com.culciful.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.dto.CommentRefRequest;
import com.culciful.dto.CommentRequest;
import com.culciful.dto.CommentSearchRequest;
import com.culciful.dto.PageSearchRequest;
import com.culciful.mapper.BlogCommentMapper;
import com.culciful.mapper.BlogMapper;
import com.culciful.mapper.FileAssetMapper;
import com.culciful.mapper.TextBodyMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.Blog;
import com.culciful.pojo.BlogComment;
import com.culciful.pojo.FileAsset;
import com.culciful.pojo.TextBody;
import com.culciful.pojo.UserInfo;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    /** 评论发布后允许编辑的时间窗（分钟）；超过或已有回复则锁定 */
    private static final int EDIT_WINDOW_MINUTES = 5;

    /** 列根评论时，每条内联返回的子回复条数（更多走「展开」） */
    private static final int PREVIEW_REPLIES = 2;

    private final BlogCommentMapper blogCommentMapper;
    private final BlogMapper blogMapper;
    private final TextBodyMapper textBodyMapper;
    private final UserInfoMapper userInfoMapper;
    private final FileAssetMapper fileAssetMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @PostMapping("/getCommentInbox")
    public R<Map<String, Object>> inbox(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        Page<BlogComment> page = blogCommentMapper.selectPage(new Page<>(request.safeCurrentPage(), request.safePageSize()),
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getAuthorId, selfId)
                        .eq(BlogComment::getIsDeleted, false)
                        .ne(BlogComment::getUserId, selfId)
                        .orderByDesc(BlogComment::getCreatedAt));
        List<Map<String, Object>> list = page.getRecords().stream().map(this::commentItem).toList();
        return R.ok(Map.of("list", list, "total", page.getTotal()));
    }

    @PostMapping("/getComments")
    public R<Map<String, Object>> search(@RequestBody @Valid CommentSearchRequest request) {
        Long aid = request.aid();
        if (aid == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        Long root = request.root();
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getBlogId, aid)
                .eq(BlogComment::getIsDeleted, false)
                .orderByAsc(BlogComment::getCreatedAt);
        if (root == null) {
            wrapper.isNull(BlogComment::getRootId);
        } else {
            wrapper.eq(BlogComment::getRootId, root);
        }
        Page<BlogComment> page = blogCommentMapper.selectPage(new Page<>(request.safeCurrentPage(), request.safePageSize()), wrapper);
        // 列根评论时，每条带前 PREVIEW_REPLIES 条子回复；列某根评论的回复时不再嵌套
        boolean listingRoots = (root == null);
        List<Map<String, Object>> list = page.getRecords().stream()
                .map(c -> commentItem(c, listingRoots))
                .toList();
        return R.ok(Map.of("list", list, "total", page.getTotal()));
    }

    @PostMapping("/addComment")
    @Transactional
    public R<Map<String, Object>> add(@RequestBody @Valid CommentRequest request) {
        Long selfId = currentUserId();
        Long aid = request.aid();
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted())) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (request == null || isBlank(request.msg())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        Long parentId = request.parent();
        Long rootId = request.root();
        if (parentId != null && rootId == null) {
            rootId = parentId;
        }
        BlogComment comment = new BlogComment();
        comment.setId(snowflakeIdGenerator.nextId());
        comment.setUserId(selfId);
        comment.setBlogId(aid);
        comment.setAuthorId(blog.getUserId());
        comment.setIsMarkdown(Boolean.TRUE.equals(request.useMD()));
        comment.setContentTextId(insertText(request.msg()));
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setIsDeleted(false);
        blogCommentMapper.insert(comment);
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .setSql("comment_count = comment_count + 1")
                .eq(Blog::getId, aid));
        return R.ok(Map.of("cid", comment.getId()));
    }

    @PostMapping("/editComment")
    public R<Void> edit(@RequestBody @Valid CommentRequest request) {
        Long selfId = currentUserId();
        Long aid = request.aid();
        Long cid = request.cid();
        BlogComment comment = cid == null ? null : blogCommentMapper.selectById(cid);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (comment == null || Boolean.TRUE.equals(comment.getIsDeleted()) || !comment.getBlogId().equals(aid)) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (!comment.getUserId().equals(selfId)) {
            return R.fail(ResultCodeEnum.FORBIDDEN);
        }
        if (request == null || isBlank(request.msg())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        if (!withinEditWindow(comment) || hasReplies(comment.getId())) {
            return R.fail(ResultCodeEnum.COMMENT_EDIT_LOCKED);
        }
        TextBody text = new TextBody();
        text.setId(comment.getContentTextId());
        text.setBody(request.msg());
        text.setContentHash(sha256(request.msg()));
        textBodyMapper.updateById(text);
        comment.setIsMarkdown(Boolean.TRUE.equals(request.useMD()));
        comment.setUpdatedAt(LocalDateTime.now());
        blogCommentMapper.updateById(comment);
        return R.ok(null);
    }

    @PostMapping("/deleteComment")
    public R<Void> delete(@RequestBody @Valid CommentRefRequest request) {
        Long selfId = currentUserId();
        Long aid = request.aid();
        Long cid = request.cid();
        BlogComment comment = cid == null ? null : blogCommentMapper.selectById(cid);
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (comment == null || blog == null || Boolean.TRUE.equals(comment.getIsDeleted()) || !comment.getBlogId().equals(aid)) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (!comment.getUserId().equals(selfId) && !blog.getUserId().equals(selfId)) {
            return R.fail(ResultCodeEnum.FORBIDDEN);
        }
        blogCommentMapper.update(null, new LambdaUpdateWrapper<BlogComment>()
                .set(BlogComment::getIsDeleted, true)
                .set(BlogComment::getUpdatedAt, LocalDateTime.now())
                .eq(BlogComment::getId, cid));
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .setSql("comment_count = greatest(comment_count - 1, 0)")
                .eq(Blog::getId, aid));
        return R.ok(null);
    }

    private Map<String, Object> commentItem(BlogComment comment) {
        return commentItem(comment, false);
    }

    private Map<String, Object> commentItem(BlogComment comment, boolean withPreviewReplies) {
        Blog blog = blogMapper.selectById(comment.getBlogId());
        TextBody text = textBodyMapper.selectById(comment.getContentTextId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cid", comment.getId());
        m.put("aid", comment.getBlogId());
        m.put("authorId", comment.getAuthorId());
        m.put("title", blog == null ? "" : blog.getTitle());
        m.put("createTime", epoch(comment.getCreatedAt()));
        m.put("updateTime", epoch(comment.getUpdatedAt()));
        m.put("useMD", Boolean.TRUE.equals(comment.getIsMarkdown()));
        // content.member = 「回复的回复」时被 @ 的人（即父评论作者）；父评论即使被删也照常显示 @
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("msg", text == null ? "" : text.getBody());
        if (comment.getParentId() != null && !comment.getParentId().equals(comment.getRootId())) {
            Long parentAuthorId = blogCommentMapper.selectAuthorIdIgnoreDeleted(comment.getParentId());
            if (parentAuthorId != null) {
                content.put("member", member(parentAuthorId));
            }
        }
        m.put("content", content);
        m.put("member", member(comment.getUserId()));
        m.put("parent", comment.getParentId());
        m.put("parentContent", parentContent(comment.getParentId()));
        m.put("root", comment.getRootId());
        long childCount = blogCommentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getRootId, comment.getId())
                .eq(BlogComment::getIsDeleted, false));
        List<Map<String, Object>> childPreview = List.of();
        if (withPreviewReplies && childCount > 0) {
            childPreview = blogCommentMapper.selectList(new LambdaQueryWrapper<BlogComment>()
                            .eq(BlogComment::getRootId, comment.getId())
                            .eq(BlogComment::getIsDeleted, false)
                            .orderByAsc(BlogComment::getCreatedAt)
                            .last("LIMIT " + PREVIEW_REPLIES))
                    .stream().map(this::commentItem).toList();
        }
        m.put("comments", Map.of("list", childPreview, "total", childCount));
        // 仅本人、在编辑时间窗内、且尚无回复时可编辑
        Long selfId = currentUserId();
        m.put("canEdit", selfId != null && selfId.equals(comment.getUserId())
                && withinEditWindow(comment) && !hasReplies(comment.getId()));
        return m;
    }

    private boolean withinEditWindow(BlogComment comment) {
        return comment.getCreatedAt() != null
                && comment.getCreatedAt().plusMinutes(EDIT_WINDOW_MINUTES).isAfter(LocalDateTime.now());
    }

    private boolean hasReplies(Long commentId) {
        return blogCommentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getParentId, commentId)
                .eq(BlogComment::getIsDeleted, false)) > 0;
    }

    private Map<String, Object> parentContent(Long parentId) {
        if (parentId == null) {
            return Map.of("msg", "");
        }
        // 逻辑删除后 selectById 返回 null；用 deleted 标记，让前端显示「评论已删除」而不是空白
        BlogComment parent = blogCommentMapper.selectById(parentId);
        if (parent == null) {
            return Map.of("msg", "", "deleted", true);
        }
        TextBody text = textBodyMapper.selectById(parent.getContentTextId());
        return Map.of("msg", text == null ? "" : text.getBody());
    }

    private Map<String, Object> member(Long userId) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            return Map.of("id", userId);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("avatarUrl", avatarUrl(user.getAvatarAssetId()));
        return m;
    }

    private long insertText(String body) {
        TextBody text = new TextBody();
        text.setId(snowflakeIdGenerator.nextId());
        text.setBody(body);
        text.setContentHash(sha256(body));
        text.setCreatedAt(LocalDateTime.now());
        textBodyMapper.insert(text);
        return text.getId();
    }

    private String avatarUrl(Long avatarAssetId) {
        if (avatarAssetId == null) {
            return null;
        }
        FileAsset asset = fileAssetMapper.selectById(avatarAssetId);
        return asset == null ? null : asset.getPublicUrl();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return longValue(auth.getName());
    }

    private long epoch(LocalDateTime time) {
        return time == null ? 0L : time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Long longValue(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
