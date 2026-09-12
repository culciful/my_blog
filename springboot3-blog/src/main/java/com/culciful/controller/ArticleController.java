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
import com.culciful.security.ArticleViewDedupeService;
import com.culciful.service.ImageStorageService;
import com.culciful.utils.ArticleAbstract;
import com.culciful.utils.ArticleCover;
import com.culciful.utils.RequestUtils;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ArticleViewDedupeService articleViewDedupeService;

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";

    @GetMapping("/getTags")
    public R<Map<String, Object>> getTags() {
        List<String> list = blogTagMapper.selectList(new LambdaQueryWrapper<BlogTag>().orderByAsc(BlogTag::getTag))
                .stream()
                .map(BlogTag::getTag)
                .toList();
        return R.ok(Map.of("list", list));
    }

    @PostMapping("/getArticleList")
    public R<Map<String, Object>> getArticleList(@RequestBody @Valid PageSearchRequest request) {
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getIsDeleted, false)
                .eq(Blog::getStatus, STATUS_PUBLISHED);
        Map<String, Object> filter = request.safeFilter();
        String keyword = request.safeKeyword();
        if (!keyword.isEmpty()) {
            wrapper.and(w -> w.like(Blog::getTitle, keyword).or().like(Blog::getAbstractText, keyword));
        }
        Long userId = asLong(filter.get("id"));
        if (userId != null) {
            wrapper.eq(Blog::getUserId, userId);
            // 作者维度（内容管理 / 个人空间）：稳定按发布时间倒序
            wrapper.orderByDesc(Blog::getCreatedAt);
        } else {
            // 主页 feed：按最新互动倒序 —— 编辑或收到新评论会把文章顶上来
            wrapper.orderByDesc(Blog::getLastActiveAt);
        }
        Long packageId = asLong(filter.get("pid"));
        if (packageId != null && packageId > 0) {
            wrapper.eq(Blog::getPackageId, packageId);
        }
        String tag = asString(filter.get("tag"));
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
    public R<Map<String, Object>> getArticleInfo(@RequestParam("aid") String aidParam, HttpServletRequest httpRequest) {
        Long aid = asLong(aidParam);
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted()) || !STATUS_PUBLISHED.equals(blog.getStatus())) {
            // 草稿对所有人（含作者）走 getArticleInfo 都是 404；作者编辑草稿走 getDraft
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        // 防刷：同一 IP 对同一篇文章，去重窗口（默认 10 分钟）内只计一次浏览量——
        // 之前是每次请求都 +1，刷新页面/被爬虫反复打就能无限堆浏览数
        if (articleViewDedupeService.shouldCount(aid, RequestUtils.clientIp(httpRequest))) {
            // 显式保留 updated_at，避免浏览量自增触发 ON UPDATE CURRENT_TIMESTAMP，
            // 让 updated_at 只反映真正的内容编辑
            blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                    .setSql("view_count = view_count + 1, updated_at = updated_at")
                    .eq(Blog::getId, aid));
            blog.setViewCount(blog.getViewCount() == null ? 1 : blog.getViewCount() + 1);
        }
        return R.ok(articleDetail(blog));
    }

    @PostMapping("/addArticle")
    @Transactional
    public R<Map<String, Long>> addArticle(@RequestBody @Valid ArticleRequest request) {
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
        applyAbstract(blog, request);
        blog.setCoverUrl(ArticleCover.firstImage(request.content()));
        blog.setContentTextId(textId);
        blog.setViewCount(0);
        blog.setCommentCount(0);
        blog.setPackageId(request.pid() == null || request.pid() == 0 ? null : request.pid());
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        blog.setLastActiveAt(LocalDateTime.now());
        blog.setIsDeleted(false);
        blog.setStatus(STATUS_PUBLISHED);
        blogMapper.insert(blog);
        replaceTags(blogId, request.tags());
        return R.ok(Map.of("id", blogId));
    }

    @PostMapping("/editArticle")
    @Transactional
    public R<Map<String, Long>> editArticle(@RequestBody @Valid ArticleRequest request) {
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
        applyAbstract(blog, request);
        blog.setCoverUrl(ArticleCover.firstImage(request.content()));
        blog.setPackageId(request.pid() == null || request.pid() == 0 ? null : request.pid());
        blog.setUpdatedAt(LocalDateTime.now());
        blog.setLastActiveAt(LocalDateTime.now());
        if (STATUS_DRAFT.equals(blog.getStatus())) {
            // 草稿 → 发布：状态翻转，发布时间取现在。文章数是实时 COUNT（见
            // UserController.getMyStats），这里不用再手动 +1
            blog.setStatus(STATUS_PUBLISHED);
            blog.setCreatedAt(LocalDateTime.now());
        }
        blogMapper.updateById(blog);
        forceCoverUrl(blog);
        replaceTags(aid, request.tags());
        return R.ok(Map.of("id", aid));
    }

    /**
     * 保存草稿：新建（无 aid）或更新自己的草稿（aid 指向自己的草稿行）。
     * 校验放松 —— 只要求标题非空，其余字段（正文/分组/标签/摘要）都可空，方便随手存。
     * 文章数统计只算 status=published 的行，草稿不计入（见 UserController.getMyStats）。
     */
    @PostMapping("/saveDraft")
    @Transactional
    public R<Map<String, Long>> saveDraft(@RequestBody ArticleRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (request == null || isBlank(request.title())
                || request.title().length() > 64
                || (request.content() != null && request.content().length() > 100_000)
                || (request.abstractText() != null && request.abstractText().length() > 200)) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        String content = request.content() == null ? "" : request.content();
        Long pkg = request.pid() == null || request.pid() == 0 ? null : request.pid();

        if (request.aid() != null) {
            Blog draft = blogMapper.selectById(request.aid());
            if (draft == null || Boolean.TRUE.equals(draft.getIsDeleted()) || !STATUS_DRAFT.equals(draft.getStatus())) {
                return R.fail(ResultCodeEnum.NOT_FOUND);
            }
            if (!draft.getUserId().equals(selfId)) {
                return R.fail(ResultCodeEnum.FORBIDDEN);
            }
            TextBody text = new TextBody();
            text.setId(draft.getContentTextId());
            text.setBody(content);
            text.setContentHash(sha256(content));
            textBodyMapper.updateById(text);
            draft.setTitle(request.title());
            applyAbstract(draft, request);
            draft.setCoverUrl(ArticleCover.firstImage(request.content()));
            draft.setPackageId(pkg);
            draft.setUpdatedAt(LocalDateTime.now());
            blogMapper.updateById(draft);
            forceCoverUrl(draft);
            replaceTags(draft.getId(), request.tags());
            return R.ok(Map.of("id", draft.getId()));
        }

        long textId = insertText(content);
        long blogId = snowflakeIdGenerator.nextId();
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setUserId(selfId);
        blog.setTitle(request.title());
        applyAbstract(blog, request);
        blog.setCoverUrl(ArticleCover.firstImage(request.content()));
        blog.setContentTextId(textId);
        blog.setViewCount(0);
        blog.setCommentCount(0);
        blog.setPackageId(pkg);
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        blog.setLastActiveAt(LocalDateTime.now());
        blog.setIsDeleted(false);
        blog.setStatus(STATUS_DRAFT);
        blogMapper.insert(blog);
        replaceTags(blogId, request.tags());
        return R.ok(Map.of("id", blogId));
    }

    /** 当前登录用户的草稿列表（按最后修改倒序）。 */
    @PostMapping("/getDraftList")
    public R<Map<String, Object>> getDraftList(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getIsDeleted, false)
                .eq(Blog::getStatus, STATUS_DRAFT)
                .eq(Blog::getUserId, selfId)
                .orderByDesc(Blog::getUpdatedAt);
        String keyword = request.safeKeyword();
        if (!keyword.isEmpty()) {
            wrapper.and(w -> w.like(Blog::getTitle, keyword).or().like(Blog::getAbstractText, keyword));
        }
        Page<Blog> page = blogMapper.selectPage(new Page<>(request.safeCurrentPage(), request.safePageSize()), wrapper);
        // 复用 articleListItem：前端草稿列表和文章列表同一个组件，字段形状要一致
        // （草稿的 viewCount/commentCount 库里就是 0，直接显示 0；createTime 取创建时间）
        List<Map<String, Object>> list = page.getRecords().stream().map(this::articleListItem).toList();
        return R.ok(Map.of("list", list, "total", page.getTotal()));
    }

    /** 取自己某篇草稿的完整内容供编辑页回填；非本人或非草稿 → 404。不自增浏览量。 */
    @GetMapping("/getDraft")
    public R<Map<String, Object>> getDraft(@RequestParam("aid") String aidParam) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        Long aid = asLong(aidParam);
        Blog blog = aid == null ? null : blogMapper.selectById(aid);
        if (blog == null || Boolean.TRUE.equals(blog.getIsDeleted()) || !STATUS_DRAFT.equals(blog.getStatus())) {
            return R.fail(ResultCodeEnum.NOT_FOUND);
        }
        if (!blog.getUserId().equals(selfId)) {
            return R.fail(ResultCodeEnum.FORBIDDEN);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("aid", blog.getId());
        m.put("title", blog.getTitle());
        TextBody text = textBodyMapper.selectById(blog.getContentTextId());
        m.put("content", text == null ? "" : text.getBody());
        m.put("abstract", blog.getAbstractText());
        m.put("isCustomAbstract", Boolean.TRUE.equals(blog.getIsCustomAbstract()));
        m.put("pid", blog.getPackageId());
        m.put("package", articlePackage(blog.getPackageId()));
        m.put("tags", tagsByBlog(blog.getId()));
        m.put("updateTime", toEpochSeconds(blog.getUpdatedAt()));
        return R.ok(m);
    }

    @PostMapping("/deleteArticle")
    public R<Void> deleteArticle(@RequestBody @Valid ArticleRefRequest request) {
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
        // 文章数是实时 COUNT（is_deleted=false 的行），软删之后这行自然不再计入，
        // 不用手动维护计数
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
        m.put("member", memberCard(blog.getUserId()));
        m.put("title", blog.getTitle());
        m.put("createTime", toEpochSeconds(blog.getCreatedAt()));
        m.put("viewCount", blog.getViewCount() == null ? 0 : blog.getViewCount());
        m.put("commentCount", blog.getCommentCount() == null ? 0 : blog.getCommentCount());
        m.put("abstract", blog.getAbstractText());
        m.put("coverUrl", blog.getCoverUrl());
        return m;
    }

    private Map<String, Object> articleDetail(Blog blog) {
        Map<String, Object> m = articleListItem(blog);
        m.put("updateTime", toEpochSeconds(blog.getUpdatedAt()));
        TextBody text = textBodyMapper.selectById(blog.getContentTextId());
        m.put("content", text == null ? "" : text.getBody());
        m.put("isCustomAbstract", Boolean.TRUE.equals(blog.getIsCustomAbstract()));
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

    private Map<String, Object> memberCard(Long userId) {
        // 作者可能已经注销了——selectById 在全局逻辑删除下会把已注销用户当不存在过滤掉
        // （文章不级联删，还得显示是谁写的），改用不受这层过滤影响的查询，
        // 前端按 isDeleted 显示「该用户已注销」而不是拿 undefined 的 username 硬拼
        UserInfo user = userInfoMapper.selectByIdIncludingDeleted(userId);
        if (user == null) {
            return Map.of("id", userId);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("avatarUrl", avatarUrl(user.getAvatarAssetId()));
        m.put("isDeleted", Boolean.TRUE.equals(user.getIsDeleted()));
        return m;
    }

    /**
     * cover_url 的更新必须无条件带上，哪怕新值是 null（正文里的图被删光了）——但 MP 默认的
     * NOT_NULL 更新策略会跳过 null 字段，updateById(blog) 那一发 SQL 顺带不到它。
     * 本来是给 Blog.coverUrl 字段加 @TableField(updateStrategy = FieldStrategy.IGNORED) 解决的，
     * 但那个注解形式在当前 JDK17.0.12 + Lombok 1.18.46 + maven-compiler-plugin 3.14.1 组合下会
     * 产出损坏的 class 文件（运行时 AnnotationFormatError: Unexpected end of annotations，
     * mvn clean 重编也复现），复现步骤：起个探针反射读 Blog 每个字段的 getDeclaredAnnotations()，
     * 只有带 updateStrategy= 的这个字段炸；去掉 updateStrategy 立刻正常——改走这个显式二次
     * update，绕开那个注解形式，行为等价（单独一条 SQL 强制写 cover_url，不管新值是不是 null）。
     */
    private void forceCoverUrl(Blog blog) {
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .eq(Blog::getId, blog.getId())
                .set(Blog::getCoverUrl, blog.getCoverUrl()));
    }

    private void replaceTags(Long blogId, List<String> tags) {
        blogTagRelationMapper.delete(new LambdaQueryWrapper<BlogTagRelation>().eq(BlogTagRelation::getBlogId, blogId));
        if (tags == null) {
            return;
        }
        for (String raw : tags) {
            String tag = asString(raw);
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
        return asLong(auth.getName());
    }

    private long toEpochSeconds(LocalDateTime time) {
        // 存储用系统时区（LocalDateTime.now()），按同一时区换算成绝对时间戳，前端再按浏览器时区显示
        return time == null ? 0L : time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /** 请求里提交了摘要就用它（标记 custom），没提交就从正文自动生成 */
    private void applyAbstract(Blog blog, ArticleRequest request) {
        String abstractText = request.abstractText();
        boolean isCustom = abstractText != null && !abstractText.isBlank();
        blog.setIsCustomAbstract(isCustom);
        blog.setAbstractText(isCustom
                ? ArticleAbstract.fromAuthor(abstractText)
                : ArticleAbstract.auto(request.content()));
    }

    private String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String asString(Object raw) {
        return raw == null ? "" : raw.toString().trim();
    }

    private static Long asLong(Object raw) {
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
