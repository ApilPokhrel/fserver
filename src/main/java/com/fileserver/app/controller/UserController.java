package com.fileserver.app.controller;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import com.fileserver.app.dao.user.UserInterface;
import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.user.User;
import com.fileserver.app.entity.user.UserDto;
import com.fileserver.app.service.AuthService;
import com.fileserver.app.util.TokenUtil;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private UserInterface userInterface;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private UserRolesInterface rolesInterface;
    private AuthService authService;

    public UserController(UserInterface userInterface, BCryptPasswordEncoder bCryptPasswordEncoder,
            UserRolesInterface rolesInterface, AuthService authService) {
        this.userInterface = userInterface;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.rolesInterface = rolesInterface;
        this.authService = authService;
    }

    @PostMapping("/")
    @PreAuthorize("permitAll()")
    public User add(@Valid @RequestBody UserDto user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userInterface.add(user).orElseThrow();
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        User user = authService.login(body.get("username"), body.get("password"));
        Map<String, Object> res = new HashMap<>();
        res.put("user", user);
        res.put("token", TokenUtil.create(user.get_id(), user.queryPerms(rolesInterface)));
        return res;
    }

    @Secured("ROLE_user_read")
    @GetMapping("/{username}")
    public User get(@RequestParam("username") String username) {
        return userInterface.getByContact(username).orElseThrow();
    }

}
