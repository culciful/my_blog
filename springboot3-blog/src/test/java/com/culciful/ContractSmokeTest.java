package com.culciful;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
