package org.example.nbauthservice.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        System.out.println("FILTER START: " + path);
        System.out.println("Method: " + request.getMethod());

        String header = request.getHeader("Authorization");
        System.out.println("Authorization header: " + (header != null ? "EXISTS" : "NULL"));

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            System.out.println("Token extracted: " + token.substring(0, Math.min(20, token.length())) + "...");
            try {
                boolean isValid = jwtTokenProvider.validateToken(token);
                System.out.println("Token valid: " + isValid);

                if (isValid) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("Authentication set: " + auth.getName());
                } else {
                    System.out.println("Token validation FAILED");
                }
            } catch (Exception e) {
                System.err.println("JWT validation ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No Bearer token in request");
        }

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(" Current authentication: " + (currentAuth != null ? currentAuth.getName() : "NULL"));


        filterChain.doFilter(request, response);
    }
}
