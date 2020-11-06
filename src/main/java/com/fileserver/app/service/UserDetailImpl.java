package com.fileserver.app.service;

import java.util.Collection;
import java.util.stream.Collectors;

import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailImpl implements UserDetails {

    /**
     *
     */
    private static final long serialVersionUID = 109809808L;

    @Autowired
    private UserRolesInterface userRolesInterface;

    private User user;

    public UserDetailImpl(User user) {
        this.user = user;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.queryPerms(userRolesInterface)
        .stream()
        .map(e -> new SimpleGrantedAuthority("ROLE_" + e))
        .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.get_id();
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
