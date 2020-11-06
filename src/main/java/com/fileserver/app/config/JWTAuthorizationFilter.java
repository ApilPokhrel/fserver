package com.fileserver.app.config;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    public JWTAuthorizationFilter(AuthenticationManager authManager) {
        super(authManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String header = req.getHeader(SecurityConstants.HEADER_STRING.toLowerCase());

        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(req);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        if(authentication != null && authentication.getPrincipal() != null){
            req.setAttribute("user", authentication.getPrincipal());
        }
        chain.doFilter(req, res);
    }


    // Reads the JWT from the Authorization header, and then uses JWT to validate
    // the token
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.HEADER_STRING.toLowerCase());
        if (token != null) {
            // parse the token.
            DecodedJWT jwt = JWT.require(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes())).build()
                    .verify(token.replace(SecurityConstants.TOKEN_PREFIX, ""));

            String id = jwt.getSubject();
            List<SimpleGrantedAuthority> perms = jwt.getClaim("perms")
                                                    .asList(String.class).stream()
                                                    .map(e -> new SimpleGrantedAuthority("ROLE_" + e))
                                                    .collect(Collectors.toList());
             if (id != null) {
                // new arraylist means authorities
                return new UsernamePasswordAuthenticationToken(id, null, perms);
            }

            return null;
        }

        return null;
    }

}