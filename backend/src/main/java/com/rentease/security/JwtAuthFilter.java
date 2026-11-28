package com.rentease.security;

import com.rentease.entity.User;
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
import java.util.Optional;
import java.util.Set;

/** Reads the Bearer token, checks it, and signs the user in for this request only. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** What a user with a temporary password may still do (FR006): change it, look at their account, or log out. */
    private static final Set<String> ALLOWED_WITH_TEMPORARY_PASSWORD =
            Set.of("/api/auth/change-password", "/api/auth/me", "/api/auth/logout");

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
                    Optional<User> match = users.findByEmail(claims.getSubject())
                            .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                            .filter(u -> tokenVersion != null && tokenVersion == u.getTokenVersion());
                    if (match.isPresent()) {
                        User u = match.get();
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        if (u.isMustChangePassword()
                                && !ALLOWED_WITH_TEMPORARY_PASSWORD.contains(request.getRequestURI())) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\":\"You must change your temporary password before you continue.\"}");
                            return;
                        }
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid, expired, or tampered token: leave the request unauthenticated (401 later).
            }
        }
        chain.doFilter(request, response);
    }
}
