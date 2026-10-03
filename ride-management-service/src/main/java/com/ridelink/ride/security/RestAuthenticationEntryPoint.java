package com.ridelink.ride.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 401 for missing, invalid or expired tokens. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String INVALID_TOKEN_ATTRIBUTE = "ridelink.invalidToken";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        boolean invalidToken = request.getAttribute(INVALID_TOKEN_ATTRIBUTE) != null;
        String message = invalidToken ? "Invalid or expired token"
                : "Authentication required: send the header 'Authorization: Bearer <token>'";
        JsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
    }
}
