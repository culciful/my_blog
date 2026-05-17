package com.culciful.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.dto.CommentRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final BlogCommentMapper blogCommentMapper;
    private final BlogMapper blogMapper;
    private final TextBodyMapper textBodyMapper;
    private final UserInfoMapper userInfoMapper;
    private final FileAssetMapper fileAssetMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @PostMapping("/comments/inbox/search")
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

    @PostMapping("/articles/{aid:\\d+}/comments/search")
    public R<Map<String, Object>> search(@PathVariable("aid") String aidParam, @RequestBody @Valid PageSearchRequest request) {
        Long aid = longValue(aidParam);
        if (aid == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        Long root = longValue(request.safeFilter().get("root"));
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
        List<Map<String, Object>> list = page.getRecords().stream().map(this::commentItem).toList();
        return R.ok(Map.of("list", list, "total", page.getTotal()));
    }

    @PostMapping("/articles/{aid:\\d+}/comments")
    @Transactional
    public R<Void> add(@PathVariable("aid") String aidParam, @RequestBody @Valid CommentRequest request) {
        Long selfId = currentUserId();
        Long aid = longValue(aidParam);
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted())) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (request == null || isBlank(request.content())) {
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
        comment.setContentTextId(insertText(request.content()));
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setIsDeleted(false);
        blogCommentMapper.insert(comment);
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .setSql("comment_count = comment_count + 1")
                .eq(Blog::getId, aid));
        return R.ok(null);
    }

    @PatchMapping("/articles/{aid:\\d+}/comments/{cid:\\d+}")
    public R<Void> edit(@PathVariable("aid") String aidParam, @PathVariable("cid") String cidParam,
                        @RequestBody @Valid CommentRequest request) {
        Long selfId = currentUserId();
        Long aid = longValue(aidParam);
        Long cid = longValue(cidParam);
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
        if (request == null || isBlank(request.content())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        TextBody text = new TextBody();
        text.setId(comment.getContentTextId());
        text.setBody(request.content());
        text.setContentHash(sha256(request.content()));
        textBodyMapper.updateById(text);
        comment.setIsMarkdown(Boolean.TRUE.equals(request.useMD()));
        comment.setUpdatedAt(LocalDateTime.now());
        blogCommentMapper.updateById(comment);
        return R.ok(null);
    }

    @DeleteMapping("/articles/{aid:\\d+}/comments/{cid:\\d+}")
    public R<Void> delete(@PathVariable("aid") String aidParam, @PathVariable("cid") String cidParam) {
        Long selfId = currentUserId();
        Long aid = longValue(aidParam);
        Long cid = longValue(cidParam);
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
        Blog blog = blogMapper.selectById(comment.getBlogId());
        TextBody text = textBodyMapper.selectById(comment.getContentTextId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cid", comment.getId());
        m.put("aid", comment.getBlogId());
        m.put("authorId", comment.getAuthorId());
        m.put("title", blog == null ? "" : blog.getTitle());
        m.put("createTime", epoch(comment.getCreatedAt()));
        m.put("useMD", Boolean.TRUE.equals(comment.getIsMarkdown()));
        m.put("content", Map.of("msg", text == null ? "" : text.getBody()));
        m.put("member", member(comment.getUserId()));
        m.put("parent", comment.getParentId());
        m.put("root", comment.getRootId());
        long childCount = blogCommentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getRootId, comment.getId())
                .eq(BlogComment::getIsDeleted, false));
        m.put("comments", Map.of("list", List.of(), "total", childCount));
        return m;
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
        return time == null ? 0L : time.toEpochSecond(ZoneOffset.UTC);
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
