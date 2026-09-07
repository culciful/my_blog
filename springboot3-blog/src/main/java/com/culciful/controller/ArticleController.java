package com.culciful.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.dto.ArticleRefRequest;
import com.culciful.dto.ArticleRequest;
import com.culciful.dto.PageSearchRequest;
import com.culciful.mapper.BlogMapper;
import com.culciful.mapper.BlogTagMapper;
import com.culciful.mapper.BlogTagRelationMapper;
import com.culciful.mapper.FileAssetMapper;
import com.culciful.mapper.TextBodyMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.mapper.UserPackageMapper;
import com.culciful.pojo.Blog;
import com.culciful.pojo.BlogTag;
import com.culciful.pojo.BlogTagRelation;
import com.culciful.pojo.FileAsset;
import com.culciful.pojo.TextBody;
import com.culciful.pojo.UserInfo;
import com.culciful.pojo.UserPackage;
import com.culciful.service.ImageStorageService;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticleController {

    private final BlogMapper blogMapper;
    private final TextBodyMapper textBodyMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogTagRelationMapper blogTagRelationMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserPackageMapper userPackageMapper;
    private final FileAssetMapper fileAssetMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ImageStorageService imageStorageService;

    @GetMapping("/getTags")
    public R<Map<String, Object>> tags() {
        List<String> list = blogTagMapper.selectList(new LambdaQueryWrapper<BlogTag>().orderByAsc(BlogTag::getTag))
                .stream()
                .map(BlogTag::getTag)
                .toList();
        return R.ok(Map.of("list", list));
    }

    @PostMapping("/getArticleList")
    public R<Map<String, Object>> search(@RequestBody @Valid PageSearchRequest request) {
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getIsDeleted, false)
                .orderByDesc(Blog::getCreatedAt);
        Map<String, Object> filter = request.safeFilter();
        String keyword = stringFilter(filter.get("keyword"));
        if (!keyword.isEmpty()) {
            wrapper.and(w -> w.like(Blog::getTitle, keyword).or().like(Blog::getOverview, keyword));
        }
        Long userId = longFilter(filter.get("id"));
        if (userId != null) {
            wrapper.eq(Blog::getUserId, userId);
        }
        Long packageId = longFilter(filter.get("pid"));
        if (packageId != null && packageId > 0) {
            wrapper.eq(Blog::getPackageId, packageId);
        }
        String tag = stringFilter(filter.get("tag"));
        if (!tag.isEmpty()) {
            Set<Long> blogIds = blogIdsByTag(tag);
            if (blogIds.isEmpty()) {
                return R.ok(Map.of("list", List.of(), "total", 0));
            }
            wrapper.in(Blog::getId, blogIds);
        }
        Page<Blog> page = blogMapper.selectPage(new Page<>(request.safeCurrentPage(), request.safePageSize()), wrapper);
        List<Map<String, Object>> list = page.getRecords().stream().map(this::articleListItem).toList();
        return R.ok(Map.of("list", list, "total", page.getTotal()));
    }

    @GetMapping("/getArticleInfo")
    public R<Map<String, Object>> detail(@RequestParam("aid") String aidParam) {
        Long aid = longFilter(aidParam);
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted())) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        // 显式保留 updated_at，避免浏览量自增触发 ON UPDATE CURRENT_TIMESTAMP，
        // 让 updated_at 只反映真正的内容编辑
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .setSql("view_count = view_count + 1, updated_at = updated_at")
                .eq(Blog::getId, aid));
        return R.ok(articleDetail(blog));
    }

    @PostMapping("/addArticle")
    @Transactional
    public R<Map<String, Long>> add(@RequestBody @Valid ArticleRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (request == null || isBlank(request.title()) || isBlank(request.content())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        long textId = insertText(request.content());
        long blogId = snowflakeIdGenerator.nextId();
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setUserId(selfId);
        blog.setTitle(request.title());
        blog.setOverview(overview(request.content()));
        blog.setContentTextId(textId);
        blog.setViewCount(0);
        blog.setCommentCount(0);
        blog.setPackageId(request.pid() == null || request.pid() == 0 ? null : request.pid());
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        blog.setIsDeleted(false);
        blogMapper.insert(blog);
        replaceTags(blogId, request.tags());
        userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .setSql("article_count = article_count + 1")
                .eq(UserInfo::getId, selfId));
        return R.ok(Map.of("id", blogId));
    }

    @PostMapping("/editArticle")
    @Transactional
    public R<Map<String, Long>> edit(@RequestBody @Valid ArticleRequest request) {
        Long selfId = currentUserId();
        Long aid = request.aid();
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted())) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (!blog.getUserId().equals(selfId)) {
            return R.fail(ResultCodeEnum.FORBIDDEN);
        }
        if (request == null || isBlank(request.title()) || isBlank(request.content())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        TextBody text = new TextBody();
        text.setId(blog.getContentTextId());
        text.setBody(request.content());
        text.setContentHash(sha256(request.content()));
        textBodyMapper.updateById(text);
        blog.setTitle(request.title());
        blog.setOverview(overview(request.content()));
        blog.setPackageId(request.pid() == null || request.pid() == 0 ? null : request.pid());
        blog.setUpdatedAt(LocalDateTime.now());
        blogMapper.updateById(blog);
        replaceTags(aid, request.tags());
        return R.ok(Map.of("id", aid));
    }

    @PostMapping("/deleteArticle")
    public R<Void> delete(@RequestBody @Valid ArticleRefRequest request) {
        Long selfId = currentUserId();
        Long aid = request.aid();
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted())) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (!blog.getUserId().equals(selfId)) {
            return R.fail(ResultCodeEnum.FORBIDDEN);
        }
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .set(Blog::getIsDeleted, true)
                .set(Blog::getUpdatedAt, LocalDateTime.now())
                .eq(Blog::getId, aid));
        userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .setSql("article_count = greatest(article_count - 1, 0)")
                .eq(UserInfo::getId, selfId));
        return R.ok(null);
    }

    @PostMapping("/uploadImage")
    public R<Map<String, String>> uploadImage(MultipartFile file) throws Exception {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        FileAsset asset = imageStorageService.store(file, selfId, ImageStorageService.Kind.ARTICLE_IMAGE);
        return R.ok(Map.of("url", asset.getPublicUrl(), "imgUrl", asset.getPublicUrl()));
    }

    private Map<String, Object> articleListItem(Blog blog) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("aid", blog.getId());
        m.put("id", blog.getUserId());
        m.put("member", member(blog.getUserId()));
        m.put("title", blog.getTitle());
        m.put("createTime", epoch(blog.getCreatedAt()));
        m.put("viewCount", blog.getViewCount() == null ? 0 : blog.getViewCount());
        m.put("commentCount", blog.getCommentCount() == null ? 0 : blog.getCommentCount());
        m.put("abstract", blog.getOverview());
        return m;
    }

    private Map<String, Object> articleDetail(Blog blog) {
        Map<String, Object> m = articleListItem(blog);
        m.put("updateTime", epoch(blog.getUpdatedAt()));
        TextBody text = textBodyMapper.selectById(blog.getContentTextId());
        m.put("content", text == null ? "" : text.getBody());
        m.put("pid", blog.getPackageId());
        m.put("package", articlePackage(blog.getPackageId()));
        m.put("tags", tagsByBlog(blog.getId()));
        m.put("comments", Map.of("list", List.of(), "total", blog.getCommentCount() == null ? 0 : blog.getCommentCount()));
        return m;
    }

    private Map<String, Object> articlePackage(Long packageId) {
        if (packageId == null || packageId == 0) {
            return Map.of("pid", 0, "pname", "All");
        }
        UserPackage userPackage = userPackageMapper.selectById(packageId);
        if (userPackage == null) {
            return Map.of("pid", packageId, "pname", "");
        }
        return Map.of("pid", userPackage.getId(), "pname", userPackage.getPackName());
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

    private void replaceTags(Long blogId, List<String> tags) {
        blogTagRelationMapper.delete(new LambdaQueryWrapper<BlogTagRelation>().eq(BlogTagRelation::getBlogId, blogId));
        if (tags == null) {
            return;
        }
        for (String raw : tags) {
            String tag = stringFilter(raw);
            if (tag.isEmpty()) {
                continue;
            }
            Long tagId = tagId(tag);
            BlogTagRelation relation = new BlogTagRelation();
            relation.setId(snowflakeIdGenerator.nextId());
            relation.setBlogId(blogId);
            relation.setTagId(tagId);
            blogTagRelationMapper.insert(relation);
        }
    }

    private Long tagId(String tag) {
        BlogTag existing = blogTagMapper.selectOne(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getTag, tag).last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        BlogTag created = new BlogTag();
        created.setId(snowflakeIdGenerator.nextId());
        created.setTag(tag);
        blogTagMapper.insert(created);
        return created.getId();
    }

    private List<String> tagsByBlog(Long blogId) {
        List<BlogTagRelation> relations = blogTagRelationMapper.selectList(new LambdaQueryWrapper<BlogTagRelation>()
                .eq(BlogTagRelation::getBlogId, blogId));
        List<String> tags = new ArrayList<>();
        for (BlogTagRelation relation : relations) {
            BlogTag tag = blogTagMapper.selectById(relation.getTagId());
            if (tag != null) {
                tags.add(tag.getTag());
            }
        }
        return tags;
    }

    private Set<Long> blogIdsByTag(String tag) {
        BlogTag existing = blogTagMapper.selectOne(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getTag, tag).last("LIMIT 1"));
        if (existing == null) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        blogTagRelationMapper.selectList(new LambdaQueryWrapper<BlogTagRelation>()
                        .eq(BlogTagRelation::getTagId, existing.getId()))
                .forEach(r -> ids.add(r.getBlogId()));
        return ids;
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
        return longFilter(auth.getName());
    }

    private long epoch(LocalDateTime time) {
        // 存储用系统时区（LocalDateTime.now()），按同一时区换算成绝对时间戳，前端再按浏览器时区显示
        return time == null ? 0L : time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private String overview(String content) {
        String text = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return text.length() <= 120 ? text : text.substring(0, 120);
    }

    private String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String stringFilter(Object raw) {
        return raw == null ? "" : raw.toString().trim();
    }

    private static Long longFilter(Object raw) {
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
