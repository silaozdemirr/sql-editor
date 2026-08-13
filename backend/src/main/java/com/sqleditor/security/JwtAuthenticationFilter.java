package com.sqleditor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    public JwtAuthenticationFilter(JwtService jwt) { this.jwt = jwt; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) try {
            Map<String, Object> claims = jwt.verify(header.substring(7));
            String role = String.valueOf(claims.get("role"));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(claims.get("sub"), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        } catch (IllegalArgumentException ignored) { }
        chain.doFilter(request, response);
    }
}
