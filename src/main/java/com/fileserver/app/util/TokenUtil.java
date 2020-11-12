package com.fileserver.app.util;

import java.util.Date;
import java.util.List;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fileserver.app.config.SecurityConstants;

public class TokenUtil {

    private TokenUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String create(String id, List<String> perms){
        return JWT.create()
        .withSubject(id)
        .withClaim("perms", perms)
        .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
        .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));
    }


    public static String create(String id, long exp, List<String> perms){
        return JWT.create()
        .withSubject(id)
        .withClaim("perms", perms)
        .withExpiresAt(new Date(System.currentTimeMillis() + exp))
        .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));
    }
}
