package com.example.Joinify.util;

import com.example.Joinify.dto.RegisterRequest;
import com.example.Joinify.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User registerRequestToUser(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            return null;
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setRole(registerRequest.getRole());

        return user;
    }

    public RegisterRequest userToRegisterRequest(User user) {
        if (user == null) {
            return null;
        }

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(user.getUsername());
        registerRequest.setEmail(user.getEmail());
        registerRequest.setPassword(user.getPassword());
        registerRequest.setRole(user.getRole());

        return registerRequest;
    }
}
