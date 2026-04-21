package com.jes.devlearn.global.security;

import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.global.security.jwt.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(AuthorizationGateTest.DummyInstructorController.class)
@DisplayName("URL 가드 - /api/instructor/** 접근 제어")
class AuthorizationGateTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TokenProvider tokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("비로그인 상태로 /api/instructor/** 접근 시 401")
    void anonymous_instructor_access_returns_401() throws Exception {
        mockMvc().perform(get("/api/instructor/__probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("STUDENT 토큰으로 /api/instructor/** 접근 시 403")
    void student_instructor_access_returns_403() throws Exception {
        com.jes.devlearn.domain.user.entity.User studentUser =
                new com.jes.devlearn.domain.user.entity.User("student01", "x", Role.STUDENT);
        org.mockito.Mockito.when(customUserDetailsService.loadUserById(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UserPrincipal(studentUser, Role.STUDENT));

        String token = tokenProvider.createToken(999L, Role.STUDENT);

        mockMvc().perform(get("/api/instructor/__probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @RestController
    @RequestMapping("/api/instructor")
    static class DummyInstructorController {
        @GetMapping("/__probe")
        ResponseEntity<String> probe() {
            return ResponseEntity.ok("ok");
        }
    }
}
