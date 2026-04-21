package com.jes.devlearn.global.security.jwt;

public final class TokenConstants {
    private TokenConstants() {}

    public static final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 5;
    public static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7;
}
