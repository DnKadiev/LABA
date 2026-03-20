package com.autoservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // Нет заголовка — пропускаем, Spring Security сам решит что вернуть
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        try {
            Claims claims = jwtTokenProvider.parseToken(token);

            // Для доступа к ресурсам принимаем только access-токен
            if (!jwtTokenProvider.isAccessToken(claims)) {
                log.warn("[JWT] Rejected non-access token (type={}) for URI={}",
                        claims.get("type"), request.getRequestURI());
                sendUnauthorized(response, "Refresh token cannot be used for API access. Please use access token.");
                return;
            }

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (role == null || role.isBlank()) {
                log.warn("[JWT] Token for user '{}' has no 'role' claim. URI={}", username, request.getRequestURI());
                sendUnauthorized(response, "Token has no role claim. Please login again.");
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("[JWT] Authenticated: user='{}' role='{}' URI={}", username, role, request.getRequestURI());

        } catch (JwtException e) {
            log.warn("[JWT] Token rejected for URI={}: {}", request.getRequestURI(), e.getMessage());
            sendUnauthorized(response, "Token invalid or expired: " + e.getMessage());
            return;
        } catch (Exception e) {
            log.error("[JWT] Unexpected error processing token for URI={}: {}", request.getRequestURI(), e.getMessage(), e);
            sendUnauthorized(response, "Token processing error. Please login again.");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
