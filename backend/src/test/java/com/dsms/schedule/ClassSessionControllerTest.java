package com.dsms.schedule;

import com.dsms.config.SecurityConfig;
import com.dsms.auth.JwtAuthenticationFilter;
import com.dsms.auth.JwtService;
import com.dsms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClassSessionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ClassSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassSessionRepository repository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void scheduleIsPubliclyAvailable() throws Exception {
        when(repository.findByStatusAndStartAtAfterOrderByStartAtAsc(
                eq(ClassStatus.PUBLISHED),
                any()
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/classes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
