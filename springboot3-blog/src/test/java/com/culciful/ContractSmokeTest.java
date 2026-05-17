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
        mockMvc.perform(post("/article/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"demo\",\"content\":\"body\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }

    @Test
    void privateUserEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/user/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(-10004));
    }
}
