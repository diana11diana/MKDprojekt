package com.dsms.auth;

import com.dsms.auth.AuthDtos.*;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import com.dsms.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            AccountTokenRepository accountTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                normalizePhone(request.phone()),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);

        String rawToken = randomToken();
        accountTokenRepository.save(new AccountToken(
                user,
                TokenType.EMAIL_VERIFICATION,
                hash(rawToken),
                Instant.now().plus(properties.verificationTokenHours(), ChronoUnit.HOURS)
        ));

        return new RegisterResponse(
                "Registration completed. Confirm your email.",
                properties.exposeVerificationToken() ? rawToken : null
        );
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountToken token = accountTokenRepository
                .findByTokenHashAndType(hash(rawToken), TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (!token.isUsable()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Token is expired or already used");
        }

        token.getUser().verifyEmail();
        token.markUsed();
    }

    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);

        if (user.isTemporarilyLocked()) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "Account is temporarily locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            throw invalidCredentials();
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Email is not verified");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!current.isUsable() || current.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
        }

        current.revoke();
        return issueTokens(current.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private LoginResult issueTokens(User user) {
        String refreshToken = randomToken();
        refreshTokenRepository.save(new RefreshToken(
                user,
                hash(refreshToken),
                Instant.now().plus(properties.refreshTokenDays(), ChronoUnit.DAYS)
        ));

        AuthResponse response = new AuthResponse(
                jwtService.createAccessToken(user),
                properties.accessTokenMinutes() * 60,
                UserResponse.from(user)
        );
        return new LoginResult(response, refreshToken);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    public record LoginResult(AuthResponse response, String refreshToken) {
    }
}
