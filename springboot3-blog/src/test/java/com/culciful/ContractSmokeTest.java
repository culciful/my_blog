package com.culciful;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.culciful.common.enums.AuditAction;
import com.culciful.config.JwtCookieProperties;
import com.culciful.mapper.AuditLogMapper;
import com.culciful.mapper.BlogMapper;
import com.culciful.mapper.FileAssetMapper;
import com.culciful.mapper.TextBodyMapper;
import com.culciful.mapper.UserFollowMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.AuditLog;
import com.culciful.pojo.Blog;
import com.culciful.pojo.FileAsset;
import com.culciful.pojo.TextBody;
import com.culciful.pojo.UserFollow;
import com.culciful.pojo.UserInfo;
import com.culciful.security.token.JwtHelper;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContractSmokeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtHelper jwtHelper;
    @Autowired
    private JwtCookieProperties jwtCookieProperties;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private AuditLogMapper auditLogMapper;
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private TextBodyMapper textBodyMapper;
    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private FileAssetMapper fileAssetMapper;

    @Test
    void articleWriteRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/article/addArticle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"demo\",\"content\":\"body\",\"pid\":1,\"tags\":[\"t\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void privateUserEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/user/getMyProfile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void emailExistenceReturnsBoolean() throws Exception {
        mockMvc.perform(post("/user/checkEmailExist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"contract-test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.result.isRegistered").isBoolean());
    }

    @Test
    void articleTagsReturnsList() throws Exception {
        mockMvc.perform(get("/article/getTags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.result.list").isArray());
    }

    @Test
    void articleSearchReturnsListAndTotal() throws Exception {
        mockMvc.perform(post("/article/getArticleList")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageSize\":10,\"currentPage\":1,\"filter\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.result.list").isArray())
                .andExpect(jsonPath("$.result.total").isNumber());
    }

    @Test
    void oversizedRequestBodyRejected() throws Exception {
        String huge = "{\"filter\":\"" + "a".repeat(2 * 1024 * 1024) + "\"}";
        mockMvc.perform(post("/article/getArticleList")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(huge))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.errorCode").value(-10012));
    }

    @Test
    void updateUserInfoRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/user/updateUserInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newname\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void updatePasswordRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/user/updatePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Test1234\",\"newPassword\":\"NewPass12\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void deleteAccountRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/user/deleteAccount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Test1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    /**
     * 直接打真实接口，落库校验 is_deleted 真的生效——这个坑就是靠 SQL 手测躲过去的：
     * application.yaml 把 isDeleted 配成了 mybatis-plus 全局逻辑删除字段，MP 生成
     * updateById(entity) 的 SQL 模板会把逻辑删除字段整个从 SET 子句摘掉，entity 里手动
     * set(true) 静默不生效；只有走 LambdaUpdateWrapper.set(...) 显式拼 SQL 才是真的。
     */
    @Test
    void deleteAccountActuallyPersistsSoftDelete() throws Exception {
        long id = snowflakeIdGenerator.nextId();
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setEmail("delacct-contract-" + id + "@example.com");
        user.setUsername("delacct" + (id % 1_000_000));
        user.setPassword(passwordEncoder.encode("Test12345"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsDeleted(false);
        user.setDeletedToken(0L);
        user.setTokenVersion(0L);
        userInfoMapper.insert(user);

        String jwt = jwtHelper.createToken(id, 0L);
        mockMvc.perform(post("/user/deleteAccount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Test12345\"}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));

        // 全局逻辑删除下 selectById 会自动过滤掉软删的行——查不到就是最直接的证明，
        // 跟 loadActiveUser()/authenticate() 依赖的是同一套过滤
        UserInfo after = userInfoMapper.selectById(id);
        Assertions.assertTrue(after == null || Boolean.TRUE.equals(after.getIsDeleted()));
    }

    /**
     * getMyStats 三个数全部实时 COUNT：草稿不算文章数、软删的文章不算、关注/粉丝各按
     * user_follow 里对应方向的行数
     */
    @Test
    void myStatsCountsAreLiveNotCached() throws Exception {
        long authorId = snowflakeIdGenerator.nextId();
        long otherId = snowflakeIdGenerator.nextId();
        insertTestUser(authorId, "stat-author");
        insertTestUser(otherId, "stat-other");

        // 2 篇已发布（算）+ 1 篇草稿（不算）+ 1 篇已发布但软删（不算）→ articleCount 应为 2
        insertTestBlog(authorId, "published", false);
        insertTestBlog(authorId, "published", false);
        insertTestBlog(authorId, "draft", false);
        insertTestBlog(authorId, "published", true);

        // otherId 关注 authorId（authorId 的粉丝）；authorId 关注 otherId（authorId 的关注）
        userFollowMapper.insert(newFollow(otherId, authorId));
        userFollowMapper.insert(newFollow(authorId, otherId));

        String jwt = jwtHelper.createToken(authorId, 0L);
        mockMvc.perform(get("/user/getStat").cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.result.articleCount").value(2))
                .andExpect(jsonPath("$.result.followerCount").value(1))
                .andExpect(jsonPath("$.result.followingCount").value(1));
    }

    /**
     * 浏览量防刷：同一 IP 反复打开同一篇文章，去重窗口内只计一次；换一个 IP 就能再计一次。
     * 之前是每次请求都 +1，刷新页面就能无限堆浏览数。
     */
    @Test
    void articleViewCountDedupedByIp() throws Exception {
        long authorId = snowflakeIdGenerator.nextId();
        insertTestUser(authorId, "view-author");
        long aid = insertTestBlog(authorId, "published", false);

        mockMvc.perform(get("/article/getArticleInfo").param("aid", String.valueOf(aid))
                        .with(req -> { req.setRemoteAddr("203.0.113.10"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(1));

        // 同一 IP 再打开 3 次，浏览量不应该继续涨
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/article/getArticleInfo").param("aid", String.valueOf(aid))
                            .with(req -> { req.setRemoteAddr("203.0.113.10"); return req; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.viewCount").value(1));
        }

        // 换个 IP，应该能再计一次
        mockMvc.perform(get("/article/getArticleInfo").param("aid", String.valueOf(aid))
                        .with(req -> { req.setRemoteAddr("203.0.113.20"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(2));
    }

    /**
     * 换头像应该清掉旧的 file_asset 行，不是每换一次就永久多攒一份没人再引用的图。
     */
    @Test
    void uploadAvatarDeletesOldFileAsset() throws Exception {
        long userId = snowflakeIdGenerator.nextId();
        insertTestUser(userId, "avatar-user");
        String jwt = jwtHelper.createToken(userId, 0L);

        mockMvc.perform(multipart("/user/uploadAvatar")
                        .file(testPng("avatar1.png"))
                        .cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));
        Long firstAssetId = userInfoMapper.selectById(userId).getAvatarAssetId();
        Assertions.assertNotNull(firstAssetId);
        Assertions.assertNotNull(fileAssetMapper.selectById(firstAssetId));

        mockMvc.perform(multipart("/user/uploadAvatar")
                        .file(testPng("avatar2.png"))
                        .cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));
        Long secondAssetId = userInfoMapper.selectById(userId).getAvatarAssetId();
        Assertions.assertNotNull(secondAssetId);
        Assertions.assertNotEquals(firstAssetId, secondAssetId);

        Assertions.assertNull(fileAssetMapper.selectById(firstAssetId), "旧头像的 file_asset 行应该被删掉");
        Assertions.assertNotNull(fileAssetMapper.selectById(secondAssetId));
    }

    /** 造一张最小的合法 PNG（ImageStorageService 会真的用 ImageIO 解码校验，随便几个字节过不了） */
    private MockMultipartFile testPng(String filename) throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", filename, "image/png", out.toByteArray());
    }

    private void insertTestUser(long id, String usernamePrefix) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setEmail(usernamePrefix + "-" + id + "@example.com");
        user.setUsername(usernamePrefix + (id % 1_000_000));
        user.setPassword(passwordEncoder.encode("Test12345"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsDeleted(false);
        user.setDeletedToken(0L);
        user.setTokenVersion(0L);
        userInfoMapper.insert(user);
    }

    private long insertTestBlog(long userId, String status, boolean isDeleted) {
        long textId = snowflakeIdGenerator.nextId();
        TextBody text = new TextBody();
        text.setId(textId);
        text.setBody("body");
        text.setContentHash("hash-" + textId);
        text.setCreatedAt(LocalDateTime.now());
        textBodyMapper.insert(text);

        long blogId = snowflakeIdGenerator.nextId();
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setUserId(userId);
        blog.setTitle("t");
        blog.setAbstractText("a");
        blog.setIsCustomAbstract(false);
        blog.setStatus(status);
        blog.setContentTextId(textId);
        blog.setViewCount(0);
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        blog.setLastActiveAt(LocalDateTime.now());
        blog.setIsDeleted(isDeleted);
        blogMapper.insert(blog);
        return blogId;
    }

    private UserFollow newFollow(long followerId, long followingId) {
        UserFollow follow = new UserFollow();
        follow.setId(snowflakeIdGenerator.nextId());
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setCreatedAt(LocalDateTime.now());
        follow.setUpdatedAt(LocalDateTime.now());
        return follow;
    }

    /** 登录失败要落一行 audit_log（GlobalExceptionHandler 那批日志兜底和这个是两件事：
     *  一个是"出错了要留痕"，这个是"敏感操作不管成功失败都要留痕"）。 */
    @Test
    void loginFailureIsAudited() throws Exception {
        String probeUsername = "audit-test-" + snowflakeIdGenerator.nextId();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + probeUsername + "\",\"password\":\"WrongPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10014));

        Long matched = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, AuditAction.LOGIN_FAILED.name())
                .eq(AuditLog::getDetail, probeUsername));
        Assertions.assertTrue(matched > 0);
    }

    @Test
    void resetPasswordValidatesFields() throws Exception {
        mockMvc.perform(post("/user/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"newPassword\":\"short\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10012));
    }

    /**
     * 回归测试：MethodArgumentNotValidException 默认的 toString()/getMessage() 会把校验失败的
     * 原始字段值原样带出来（"rejected value [xxx]"）——之前 GlobalExceptionHandler.handleBadRequest
     * 直接打了 e.toString()，DEBUG 级别一开就会把这里提交的明文密码写进日志。改成只打异常类名后
     * 这条测试才通过；如果以后有人手滑改回 e.toString()/e.getMessage()，这条测试会炸。
     */
    @Test
    void badRequestLogNeverContainsSubmittedPassword() throws Exception {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(
                "com.culciful.common.exception.GlobalExceptionHandler");
        Level original = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            mockMvc.perform(post("/user/resetPassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"x@example.com\",\"newPassword\":\"lEak1x\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode").value(-10012));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(original);
        }
        boolean leaked = appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("lEak1x"));
        Assertions.assertFalse(leaked, "GlobalExceptionHandler 的 DEBUG 日志把提交的密码明文打出来了");
    }

    @Test
    void imageUploadRequiresAuthentication() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/comment/uploadImage")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "x.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void draftEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/article/saveDraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"draft\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
        mockMvc.perform(post("/article/getDraftList")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageSize\":10,\"currentPage\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
        mockMvc.perform(get("/article/getDraft").param("aid", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void commentInboxRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/comment/getCommentInbox")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageSize\":10,\"currentPage\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    // dev profile 的 blog.cors.allowed-origins 含 http://localhost:5173（application-dev.yaml）

    @Test
    void corsPreflightAllowsConfiguredOriginAndContentType() throws Exception {
        mockMvc.perform(options("/article/getArticleList")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsPreflightRejectsUnlistedHeader() throws Exception {
        mockMvc.perform(options("/article/getArticleList")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Evil"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsRejectsUnlistedOrigin() throws Exception {
        mockMvc.perform(options("/article/getArticleList")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidVerificationCodePayloadReturnsParamError() throws Exception {
        mockMvc.perform(post("/user/sendEmailCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10012));
    }

    @Test
    void securityHeadersPresent() throws Exception {
        mockMvc.perform(get("/article/getTags"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                // HTTP（非 HTTPS）请求不应该带 HSTS：明文响应里发这个头没意义，还可能被剥离
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void hstsOnlySentOverHttps() throws Exception {
        mockMvc.perform(get("/article/getTags").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
    }
}
