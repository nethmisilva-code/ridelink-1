package com.ridelink.driver.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the standard RideLink error body for failures raised by the security
 * filter chain (before a controller is reached), so 401 and 403 responses have
 * exactly the same shape as errors produced by GlobalExceptionHandler.
 */
public final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + escape(status.getReasonPhrase()) + "\",\"message\":\"" + escape(message)
                + "\",\"path\":\"" + escape(path) + "\"}");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    }
                    else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
