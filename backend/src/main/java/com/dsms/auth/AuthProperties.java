package com.dsms.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        long accessTokenMinutes,
        long refreshTokenDays,
        long verificationTokenHours,
        boolean exposeVerificationToken,
        boolean secureCookie
) {
}

