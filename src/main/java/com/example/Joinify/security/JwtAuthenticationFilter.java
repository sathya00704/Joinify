package com.example.Joinify.security;

import com.example.Joinify.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);

            // Validate token format before processing
            if (jwtToken.trim().isEmpty()) {
                logger.error("JWT token is empty");
            } else if (!isValidJwtFormat(jwtToken)) {
                logger.error("JWT token format is invalid: " + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
            } else {
                try {
                    // CHANGE: Use extractUsername instead of getUsernameFromToken
                    username = jwtTokenUtil.extractUsername(jwtToken);
                } catch (IllegalArgumentException e) {
                    logger.error("Unable to get JWT Token", e);
                } catch (ExpiredJwtException e) {
                    logger.error("JWT Token has expired", e);
                } catch (MalformedJwtException e) {
                    logger.error("JWT Token is malformed", e);
                } catch (Exception e) {
                    logger.error("Cannot extract username from JWT token: " + e.getMessage());
                }
            }
        } else if (requestTokenHeader != null) {
            logger.warn("JWT Token does not begin with Bearer String");
        }

        // Once we get the token validate it.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // if token is valid configure Spring Security to manually set authentication
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }

    // Helper method to validate JWT format
    private boolean isValidJwtFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // JWT should have exactly 2 dots (3 parts: header.payload.signature)
        long dotCount = token.chars().filter(ch -> ch == '.').count();
        return dotCount == 2;
    }
}
