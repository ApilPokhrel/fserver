package com.fileserver.app.service;

import java.util.Optional;

import com.fileserver.app.dao.user.UserInterface;
import com.fileserver.app.entity.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service(value = "userService")
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserInterface userRepository;

    @Override
    public UserDetails loadUserByUsername(String username){
        Optional<User> user = userRepository.getById(username);

        if (!user.isPresent()) {
            throw new UsernameNotFoundException("Could not find user");
        }

        return new UserDetailImpl(user.get());
    }

}
