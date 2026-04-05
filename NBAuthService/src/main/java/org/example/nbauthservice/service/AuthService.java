package org.example.nbauthservice.service;

import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.JwtRequest;
import org.example.nbauthservice.dto.JwtResponse;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    private Authentication authenticate(Authentication authentication) {
        Authentication authenticated = authenticationManager.authenticate(authentication);
        return authenticated;
    }

    public JwtResponse login(JwtRequest jwtRequest) {
        authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.getUsername(), jwtRequest.getPassword()));

        User user = userService.getByUsername(jwtRequest.getUsername());

        // Теперь можно логиниться без верификации email

        JwtResponse jwtResponse = new JwtResponse();
        jwtResponse.setId(user.getId());
        jwtResponse.setName(user.getName());
        jwtResponse.setUsername(user.getUsername());
        jwtResponse.setEmail(user.getEmail());
        jwtResponse.setToken(jwtTokenProvider.createAccessToken(user));
        jwtResponse.setRefreshToken(jwtTokenProvider.createRefreshToken(user));
        jwtResponse.setOnboarded(user.isOnboarded());

        return jwtResponse;
    }

    public JwtResponse refreshToken(String refreshToken) {
        return jwtTokenProvider.refresh(refreshToken);
    }
}
//package org.example.nbauthservice.service;
//
//import lombok.RequiredArgsConstructor;
//import org.example.nbauthservice.dto.JwtRequest;
//import org.example.nbauthservice.dto.JwtResponse;
//import org.example.nbauthservice.entity.User;
//import org.example.nbauthservice.security.JwtTokenProvider;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//    private final JwtTokenProvider jwtTokenProvider;
//    private final AuthenticationManager authenticationManager;
//    private final UserService userService;
//
//    private Authentication authenticate(Authentication authentication) {
//        Authentication authenticated = authenticationManager.authenticate(authentication);
//        return authenticated;
//    }
//
//    public JwtResponse login(JwtRequest jwtRequest) {
//        authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.getUsername(), jwtRequest.getPassword()));
//
//        User user = userService.getByUsername(jwtRequest.getUsername());
//
//        if (user.getEmail() != null && !user.isEmailVerified()) {
//            throw new IllegalStateException("Email not verified. Check your inbox.");
//        }
//
//        //при указании номера телефона чтобы не блокировал свои запросом кода нужно комментить но оставить
////        if (user.getPhone() != null && !user.isPhoneVerified()) {
////            throw new IllegalStateException("Phone not verified. Enter the code sent to your phone.");
////        }
//
//        JwtResponse jwtResponse = new JwtResponse();
//        jwtResponse.setId(user.getId());
//        jwtResponse.setName(user.getName());
//        jwtResponse.setUsername(user.getUsername());
//        jwtResponse.setAccessToken(jwtTokenProvider.createAccessToken(user));
//        jwtResponse.setRefreshToken(jwtTokenProvider.createRefreshToken(user));
//
//        return jwtResponse;
//    }
//
//    public JwtResponse refreshToken(String refreshToken) {
//        return jwtTokenProvider.refresh(refreshToken);
//    }
//}