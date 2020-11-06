package com.fileserver.app.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;

import com.fileserver.app.dao.user.UserInterface;
import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.user.User;
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

    public UserController(UserInterface userInterface, BCryptPasswordEncoder bCryptPasswordEncoder, UserRolesInterface rolesInterface){
        this.userInterface = userInterface;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.rolesInterface = rolesInterface;
    }


    @PostMapping("/")
    @PreAuthorize("permitAll()")
    public User add(@Valid @RequestBody User user){
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userInterface.add(user).orElseThrow();
    }


    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body){
        Optional<User> user = userInterface.getByContact(body.get("username"));
        if(user.isPresent()){
            if(bCryptPasswordEncoder.matches(body.get("password"), user.get().getPassword())){
                Map<String, Object> res = new HashMap<>();
                res.put("user", user.get());
                res.put("token", TokenUtil.create(user.get().get_id(), user.get().queryPerms(rolesInterface)));
                return res;
            } else {
                throw new ConstraintViolationException("password didn't matched", new HashSet<>());
            }
        } else {
           throw new ConstraintViolationException("user not found", new HashSet<>());
        }
    }

    @Secured("ROLE_user_read")
    @GetMapping("/{username}")
    public User get(@RequestParam("username") String username){
        return userInterface.getByContact(username).orElseThrow();
    }

}
