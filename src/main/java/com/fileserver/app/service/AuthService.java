package com.fileserver.app.service;

import java.util.HashSet;
import java.util.Optional;

import javax.validation.ConstraintViolationException;

import com.fileserver.app.dao.user.UserInterface;
import com.fileserver.app.entity.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private UserInterface userInterface;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public AuthService(UserInterface userInterface, BCryptPasswordEncoder bCryptPasswordEncoder){
        this.userInterface = userInterface;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public User login(String username, String password){
        Optional<User> user = userInterface.getByContact(username);
        if(user.isPresent()){
            if(bCryptPasswordEncoder.matches(password, user.get().getPassword())){
                return user.get();
            } else {
                throw new ConstraintViolationException("password didn't matched", new HashSet<>());
            }
        } else {
           throw new ConstraintViolationException("user not found", new HashSet<>());
        }
    }
}
