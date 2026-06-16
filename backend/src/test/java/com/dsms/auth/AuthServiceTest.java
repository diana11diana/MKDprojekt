package com.dsms.auth;

import com.dsms.auth.AuthDtos.RegisterRequest;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountTokenRepository accountTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                "test-secret-that-is-long-enough-for-hmac-256",
                15,
                30,
                24,
                true,
                false
        );
        authService = new AuthService(
                userRepository,
                accountTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                properties
        );
    }

    @Test
    void registrationNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmail("client@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(new RegisterRequest(
                " Anna ",
                " Kowalska ",
                " CLIENT@Example.com ",
                "+48 500 100 200",
                "Password1"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(accountTokenRepository).save(any(AccountToken.class));

        User saved = userCaptor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Anna");
        assertThat(saved.getLastName()).isEqualTo("Kowalska");
        assertThat(saved.getEmail()).isEqualTo("client@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(response.verificationToken()).isNotBlank();
    }

    @Test
    void duplicateEmailReturnsConflict() {
        when(userRepository.existsByEmail("client@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "Anna",
                "Kowalska",
                "client@example.com",
                null,
                "Password1"
        )))
                .isInstanceOfSatisfying(AuthException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void invalidVerificationTokenIsRejected() {
        when(accountTokenRepository.findByTokenHashAndType(any(), eq(TokenType.EMAIL_VERIFICATION)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("wrong-token"))
                .isInstanceOfSatisfying(AuthException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}

