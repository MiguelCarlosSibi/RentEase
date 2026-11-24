package com.rentease.security;

import com.rentease.entity.UserStatus;
import com.rentease.repository.RevokedTokenRepository;
import com.rentease.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads the Bearer token, checks it, and signs the user in for this request only. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;

    public JwtAuthFilter(JwtService jwtService, UserRepository users, RevokedTokenRepository revokedTokens) {
        this.jwtService = jwtService;
        this.users = users;
        this.revokedTokens = revokedTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                final Integer tokenVersion = claims.get("ver", Integer.class);
                if (!revokedTokens.existsById(claims.getId())) {
                    users.findByEmail(claims.getSubject())
                            .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                            .filter(u -> tokenVersion != null && tokenVersion == u.getTokenVersion())
                            .ifPresent(u -> {
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            });
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid, expired, or tampered token: leave the request unauthenticated (401 later).
            }
        }
        chain.doFilter(request, response);
    }
}
