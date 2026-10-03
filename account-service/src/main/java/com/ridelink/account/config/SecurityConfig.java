package com.ridelink.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ridelink.account.security.JwtAuthFilter;
import com.ridelink.account.security.JwtService;
import com.ridelink.account.security.RestAccessDeniedHandler;
import com.ridelink.account.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/register", "/api/v1/auth/login",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/actuator/health", "/actuator/health/**", "/actuator/info", "/error" };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
            RestAuthenticationEntryPoint authenticationEntryPoint, RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Disables Spring Boot's generated in-memory user; login is handled by AuthService. */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Use POST /api/v1/auth/login");
        };
    }
}
