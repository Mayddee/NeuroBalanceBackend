package org.example.nbauthservice.security;


import org.example.nbauthservice.entity.User;

public class JwtEntityFactory {

    public static JwtEntity generateUserDetails(User user) {
        return new JwtEntity(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getPassword(),
                user.getRoles()
        );

    }
}
