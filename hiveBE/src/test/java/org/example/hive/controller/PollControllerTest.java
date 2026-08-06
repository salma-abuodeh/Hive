package org.example.hive.controller;

import org.example.hive.config.AppEnums.EventVisibility;
import org.example.hive.config.SecurityConfig;
import org.example.hive.dto.response.PollResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.security.CustomUserDetailsService;
import org.example.hive.security.JwtAuthenticationFilter;
import org.example.hive.security.SimplePermissionEvaluator;
import org.example.hive.service.PollService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PollController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SimplePermissionEvaluator.class})
class PollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PollService pollService;

    private AuthUserPrincipal mockPrincipalWithPermission;
    private AuthUserPrincipal mockPrincipalWithoutPermission;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        mockPrincipalWithPermission = new AuthUserPrincipal(
                100L,
                1L,
                "user@hive.local",
                "password",
                true,
                List.of(
                        new SimpleGrantedAuthority("POLL_VIEW"),
                        new SimpleGrantedAuthority("POLL_CREATE")
                )
        );

        mockPrincipalWithoutPermission = new AuthUserPrincipal(
                101L,
                1L,
                "unauthorized@hive.local",
                "password",
                true,
                List.of()
        );
    }

    @Test
    @DisplayName("GET /polls should return 200 OK when user has POLL_VIEW permission")
    void getPolls_Authorized() throws Exception {
        when(pollService.list(anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/polls")
                        .with(user(mockPrincipalWithPermission)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /polls should return 403 Forbidden when user lacks POLL_VIEW permission")
    void getPolls_Forbidden() throws Exception {
        mockMvc.perform(get("/polls")
                        .with(user(mockPrincipalWithoutPermission)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /polls should return 201 Created when request is valid and user has POLL_CREATE")
    void createPoll_Success() throws Exception {
        PollResponse mockResponse = new PollResponse(
                1L,
                mockPrincipalWithPermission.getCompanyId(),
                null,
                mockPrincipalWithPermission.getUserId(),
                "Test User",
                "Where should we have lunch?",
                null,
                false,
                null,
                EventVisibility.COMPANY,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0L,
                Collections.emptyList()
        );

        when(pollService.create(anyLong(), anyLong(), any()))
                .thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "question": "Where should we have lunch?",
                    "options": [
                        { "text": "Pizza" },
                        { "text": "Tacos" }
                    ]
                }
                """;

        mockMvc.perform(post("/polls")
                        .with(csrf())
                        .with(user(mockPrincipalWithPermission))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());
    }
}