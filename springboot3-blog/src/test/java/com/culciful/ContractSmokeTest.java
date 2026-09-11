package com.culciful;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.culciful.common.enums.AuditAction;
import com.culciful.config.JwtCookieProperties;
import com.culciful.mapper.AuditLogMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.AuditLog;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        user.setArticleCount(0);
        user.setFollowingCount(0);
        user.setFollowerCount(0);
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
}
