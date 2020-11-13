package com.fileserver.app.config;

public  class SecurityConstants {

    private SecurityConstants(){
        throw new IllegalStateException("Utility class");
    }

    public static final String SECRET = "SECRET_KEY_FOR_MY_JWT_MANN_HAA_MANN";
    public static final long EXPIRATION_TIME = 9000000000L; // 15 mins
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String SIGN_UP_URL = "/api/v1/user/";
}
