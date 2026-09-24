package com.culciful;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.culciful.config.JwtCookieProperties;
import com.culciful.config.UploadProperties;
import com.culciful.mapper.BlogCommentMapper;
import com.culciful.mapper.BlogMapper;
import com.culciful.mapper.EmailVerificationCodeMapper;
import com.culciful.mapper.TextBodyMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.mapper.UserPackageMapper;
import com.culciful.pojo.Blog;
import com.culciful.pojo.BlogComment;
import com.culciful.pojo.EmailVerificationCode;
import com.culciful.pojo.TextBody;
import com.culciful.pojo.UserInfo;
import com.culciful.pojo.UserPackage;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段3集成测试：真打接口 + 查库断言，不是纯"要求登录"的表层检查。分五块：
 * auth 全流程、JWT 解析健壮性（原计划里的"加密解析"——传输层 RSA 已在阶段1移除，
 * 这块剩下的加密/解析面就是 JWT 签名校验 + token_version）、上传校验、验证码限流、
 * 越权（改他人文章/评论/package）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class IntegrationSmokeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private TextBodyMapper textBodyMapper;
    @Autowired
    private BlogCommentMapper blogCommentMapper;
    @Autowired
    private UserPackageMapper userPackageMapper;
    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtHelper jwtHelper;
    @Autowired
    private JwtCookieProperties jwtCookieProperties;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private UploadProperties uploadProperties;

    // ========================================================================
    // 1. auth 全流程：注册（真验证码）→ 登录 → 访问受保护接口 → 改密码 → 旧 token
    //    立即失效（哪怕签名和有效期都没问题）→ 用新密码重新登录成功
    // ========================================================================

    @Test
    void authFullFlow_registerLoginChangePasswordInvalidatesOldToken() throws Exception {
        long marker = snowflakeIdGenerator.nextId();
        String email = "auth-flow-" + marker + "@example.com";
        String username = "authflow" + (marker % 1_000_000);
        insertValidEmailCode(email, "register", "123456");

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"username\":\"" + username
                                + "\",\"password\":\"Test12345\",\"verificationCode\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Test12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andReturn();
        Cookie oldJwt = loginResult.getResponse().getCookie(jwtCookieProperties.getName());
        Assertions.assertNotNull(oldJwt, "登录应该下发 JWT cookie");

        mockMvc.perform(get("/user/getMyProfile").cookie(oldJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.result.username").value(username));

        mockMvc.perform(post("/user/updatePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Test12345\",\"newPassword\":\"NewPass123\"}")
                        .cookie(oldJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));

        // token_version+1，旧 cookie 签名/有效期都没问题，但版本号对不上，必须立即失效
        mockMvc.perform(get("/user/getMyProfile").cookie(oldJwt))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));

        MvcResult reLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"NewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andReturn();
        Cookie newJwt = reLoginResult.getResponse().getCookie(jwtCookieProperties.getName());
        Assertions.assertNotNull(newJwt);
        mockMvc.perform(get("/user/getMyProfile").cookie(newJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));
    }

    // ========================================================================
    // 2. JWT 解析健壮性（原计划里"加密解析"对应的部分——传输层 RSA 已在阶段1移除，
    //    现在密码走明文 JSON + HTTPS，剩下需要验证的"解析"面就是 JWT）
    // ========================================================================

    @Test
    void malformedJwtCookieTreatedAsUnauthenticated() throws Exception {
        mockMvc.perform(get("/user/getMyProfile")
                        .cookie(new Cookie(jwtCookieProperties.getName(), "not-a-real-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void staleTokenVersionRejectedDespiteValidSignature() throws Exception {
        long userId = snowflakeIdGenerator.nextId();
        insertTestUser(userId, "stale-tv");
        String jwt = jwtHelper.createToken(userId, 0L); // 签发时 tv=0，签名/有效期都合法

        // 之后用户 token_version 被别处改成了 1（改密码/改邮箱/注销都会这么干）
        userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .set(UserInfo::getTokenVersion, 1L)
                .eq(UserInfo::getId, userId));

        mockMvc.perform(get("/user/getMyProfile")
                        .cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    // ========================================================================
    // 3. 上传校验：超限 / 非图片内容都要被真的拒掉，不是只查了"要登录"
    // ========================================================================

    @Test
    void uploadAvatarRejectsOversizedFile() throws Exception {
        long userId = snowflakeIdGenerator.nextId();
        insertTestUser(userId, "big-avatar");
        String jwt = jwtHelper.createToken(userId, 0L);
        byte[] oversized = new byte[(int) uploadProperties.getAvatarMaxSize().toBytes() + 1];
        mockMvc.perform(multipart("/user/uploadAvatar")
                        .file(new MockMultipartFile("file", "big.png", "image/png", oversized))
                        .cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10012));
    }

    @Test
    void uploadAvatarRejectsNonImageContent() throws Exception {
        long userId = snowflakeIdGenerator.nextId();
        insertTestUser(userId, "fake-avatar");
        String jwt = jwtHelper.createToken(userId, 0L);
        // 文件名/Content-Type 都伪装成 png，内容其实是脚本文本——ImageStorageService 不信
        // 客户端报的类型，靠 ImageIO 真解码，这种应该在 header 阶段就被拒
        mockMvc.perform(multipart("/user/uploadAvatar")
                        .file(new MockMultipartFile("file", "evil.png", "image/png",
                                "<script>alert(document.domain)</script>".getBytes()))
                        .cookie(new Cookie(jwtCookieProperties.getName(), jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10012));
    }

    // ========================================================================
    // 4. 验证码限流：发送冷却 + 校验失败次数上限
    // ========================================================================

    @Test
    void sendEmailCodeCooldownRateLimited() throws Exception {
        String email = "cooldown-" + snowflakeIdGenerator.nextId() + "@example.com";
        mockMvc.perform(post("/user/sendEmailCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"scene\":\"register\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));
        // 立刻再发一次，同邮箱+场景的重发冷却（默认 60s）应该拦住
        mockMvc.perform(post("/user/sendEmailCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"scene\":\"register\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10018));
    }

    @Test
    void emailCodeInvalidatedAfterTooManyFailedAttempts() throws Exception {
        String email = "lockout-" + snowflakeIdGenerator.nextId() + "@example.com";
        insertValidEmailCode(email, "register", "654321");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/user/checkEmailCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"verificationCode\":\"000000\",\"scene\":\"register\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode").value(-10012));
        }
        // 正确的码，但已经错了 5 次（默认上限），应该已经作废，不能再用它验证通过
        mockMvc.perform(post("/user/checkEmailCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"verificationCode\":\"654321\",\"scene\":\"register\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10012));
    }

    // ========================================================================
    // 5. 越权（IDOR）：改他人文章 / 评论 / package
    // ========================================================================

    @Test
    void editArticleByNonOwnerForbidden() throws Exception {
        long ownerId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(ownerId, "art-owner");
        insertTestUser(attackerId, "art-attacker");
        long aid = insertTestBlog(ownerId, "published", false);
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        mockMvc.perform(post("/article/editArticle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aid\":" + aid + ",\"title\":\"hacked\",\"content\":\"hacked body\",\"pid\":0,\"tags\":[]}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10009));

        Blog after = blogMapper.selectById(aid);
        Assertions.assertEquals("t", after.getTitle(), "非作者的编辑请求不该真的改到内容");
    }

    @Test
    void deleteArticleByNonOwnerForbidden() throws Exception {
        long ownerId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(ownerId, "del-art-owner");
        insertTestUser(attackerId, "del-art-attacker");
        long aid = insertTestBlog(ownerId, "published", false);
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        mockMvc.perform(post("/article/deleteArticle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aid\":" + aid + "}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10009));

        Blog after = blogMapper.selectById(aid);
        Assertions.assertFalse(Boolean.TRUE.equals(after.getIsDeleted()), "非作者的删除请求不该真的软删掉文章");
    }

    @Test
    void editCommentByNonOwnerForbidden() throws Exception {
        long articleAuthorId = snowflakeIdGenerator.nextId();
        long commentAuthorId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(articleAuthorId, "cm-art-author");
        insertTestUser(commentAuthorId, "cm-author");
        insertTestUser(attackerId, "cm-attacker");
        long aid = insertTestBlog(articleAuthorId, "published", false);
        long cid = insertTestComment(aid, commentAuthorId, articleAuthorId);
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        mockMvc.perform(post("/comment/editComment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aid\":" + aid + ",\"cid\":" + cid
                                + ",\"isMarkdown\":false,\"content\":{\"msg\":\"hacked\"}}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10009));
    }

    @Test
    void deleteCommentByUnrelatedUserForbidden() throws Exception {
        long articleAuthorId = snowflakeIdGenerator.nextId();
        long commentAuthorId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(articleAuthorId, "dc-art-author");
        insertTestUser(commentAuthorId, "dc-author");
        insertTestUser(attackerId, "dc-attacker");
        long aid = insertTestBlog(articleAuthorId, "published", false);
        long cid = insertTestComment(aid, commentAuthorId, articleAuthorId);
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        // attacker 既不是评论作者也不是文章作者——两个身份都不占，应该被拦
        mockMvc.perform(post("/comment/deleteComment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aid\":" + aid + ",\"cid\":" + cid + "}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10009));

        BlogComment after = blogCommentMapper.selectById(cid);
        Assertions.assertFalse(Boolean.TRUE.equals(after.getIsDeleted()));
    }

    /** 正对照：文章作者虽然不是评论作者，但可以删自己文章下的评论（版主权限，不是漏洞）。 */
    @Test
    void deleteCommentByArticleAuthorAllowed() throws Exception {
        long articleAuthorId = snowflakeIdGenerator.nextId();
        long commentAuthorId = snowflakeIdGenerator.nextId();
        insertTestUser(articleAuthorId, "mod-art-author");
        insertTestUser(commentAuthorId, "mod-cm-author");
        long aid = insertTestBlog(articleAuthorId, "published", false);
        long cid = insertTestComment(aid, commentAuthorId, articleAuthorId);
        String articleAuthorJwt = jwtHelper.createToken(articleAuthorId, 0L);

        mockMvc.perform(post("/comment/deleteComment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aid\":" + aid + ",\"cid\":" + cid + "}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), articleAuthorJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));

        // BlogComment 也在 MP 全局逻辑删除字段（isDeleted）覆盖范围内，selectById 对软删的行
        // 自动过滤——查不到就是删成功了的证明（跟 deleteAccountActuallyPersistsSoftDelete
        // 用的是同一个判断方式）
        Assertions.assertNull(blogCommentMapper.selectById(cid));
    }

    @Test
    void editPackageByNonOwnerHasNoEffect() throws Exception {
        long ownerId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(ownerId, "pkg-owner");
        insertTestUser(attackerId, "pkg-attacker");
        long pid = insertTestPackage(ownerId, "original-name");
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        // editPackage/deletePackage 要求 body 里的 id 字段等于自己（isSelf 校验），所以攻击者
        // 只能填自己的 id；再拼上受害者的 pid，WHERE 子句里 user_id=攻击者自己，查不到受害者
        // 那一行——静默"成功"（errorCode=0）但实际没有任何行被更新，不是真的越权
        mockMvc.perform(post("/user/editPackage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + attackerId + ",\"pid\":" + pid + ",\"pname\":\"hacked\"}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));

        UserPackage after = userPackageMapper.selectById(pid);
        Assertions.assertEquals("original-name", after.getPackName(), "非所有者的编辑请求不该真的改到分组名");
    }

    @Test
    void editPackageWithSomeoneElsesIdAsSelfRejected() throws Exception {
        long ownerId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(ownerId, "pkg-owner2");
        insertTestUser(attackerId, "pkg-attacker2");
        long pid = insertTestPackage(ownerId, "another-name");
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        // 换一种攻击姿势：body 里 id 直接填受害者的 id（冒充是受害者本人发的请求）——
        // isSelf(自己真实登录态, 声称的 id) 不匹配，参数校验阶段就被拒
        mockMvc.perform(post("/user/editPackage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + ownerId + ",\"pid\":" + pid + ",\"pname\":\"hacked\"}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(-10012));

        UserPackage after = userPackageMapper.selectById(pid);
        Assertions.assertEquals("another-name", after.getPackName());
    }

    @Test
    void deletePackageByNonOwnerHasNoEffect() throws Exception {
        long ownerId = snowflakeIdGenerator.nextId();
        long attackerId = snowflakeIdGenerator.nextId();
        insertTestUser(ownerId, "del-pkg-owner");
        insertTestUser(attackerId, "del-pkg-attacker");
        long pid = insertTestPackage(ownerId, "keep-me");
        String attackerJwt = jwtHelper.createToken(attackerId, 0L);

        mockMvc.perform(post("/user/deletePackage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + attackerId + ",\"pid\":" + pid + "}")
                        .cookie(new Cookie(jwtCookieProperties.getName(), attackerJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0));

        UserPackage after = userPackageMapper.selectById(pid);
        Assertions.assertFalse(Boolean.TRUE.equals(after.getIsDeleted()), "非所有者的删除请求不该真的软删掉分组");
    }

    // ========================================================================
    // helpers
    // ========================================================================

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

    private long insertTestComment(long blogId, long commentAuthorId, long articleAuthorId) {
        long textId = snowflakeIdGenerator.nextId();
        TextBody text = new TextBody();
        text.setId(textId);
        text.setBody("comment body");
        text.setContentHash("hash-" + textId);
        text.setCreatedAt(LocalDateTime.now());
        textBodyMapper.insert(text);

        long cid = snowflakeIdGenerator.nextId();
        BlogComment comment = new BlogComment();
        comment.setId(cid);
        comment.setUserId(commentAuthorId);
        comment.setBlogId(blogId);
        comment.setAuthorId(articleAuthorId);
        comment.setIsMarkdown(false);
        comment.setContentTextId(textId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setIsDeleted(false);
        blogCommentMapper.insert(comment);
        return cid;
    }

    private long insertTestPackage(long userId, String name) {
        long pid = snowflakeIdGenerator.nextId();
        UserPackage p = new UserPackage();
        p.setId(pid);
        p.setUserId(userId);
        p.setPackName(name);
        p.setIsDeleted(false);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        userPackageMapper.insert(p);
        return pid;
    }

    private void insertValidEmailCode(String email, String scene, String code) {
        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setEmail(email);
        entity.setScene(scene);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        entity.setAttemptCount(0);
        entity.setRequestIp("127.0.0.1");
        entity.setCreatedAt(LocalDateTime.now());
        emailVerificationCodeMapper.insert(entity);
    }
}
