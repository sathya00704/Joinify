package com.example.Joinify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/hello.html",           // Allow direct access to your hello page
                                "/css/**", "/js/**",     // Allow static resources (if any)
                                "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated() // All other requests require authentication
                )
                .formLogin() // Keep default login for now
                .and()
                .logout();

        return http.build();
    }
}