package com.ridelink.driver.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads "Authorization: Bearer &lt;jwt&gt;", verifies it and stores the caller in
 * the security context. Requests without a valid token stay anonymous, and
 * Spring Security then rejects them on protected paths with 401.
 * Created in SecurityConfig (not a @Component) so it is registered only once.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            try {
                AuthenticatedUser user = jwtService.parse(header.substring(BEARER.length()).trim());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            catch (InvalidTokenException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(RestAuthenticationEntryPoint.INVALID_TOKEN_ATTRIBUTE, Boolean.TRUE);
            }
        }
        chain.doFilter(request, response);
    }
}
