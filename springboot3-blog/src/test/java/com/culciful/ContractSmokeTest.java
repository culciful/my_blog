package com.culciful;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContractSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getConfReturnsRsaPublicKey() throws Exception {
        mockMvc.perform(get("/api/getConf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.result.data").isString());
    }

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
                .andExpect(jsonPath("$.result.isExisted").isBoolean());
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
    void commentInboxRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/comment/getCommentInbox")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageSize\":10,\"currentPage\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
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
