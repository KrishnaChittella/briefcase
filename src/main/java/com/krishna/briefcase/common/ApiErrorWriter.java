package com.krishna.briefcase.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Writes a minimal {@link ApiError}-shaped JSON body directly to the response, for use in
 * Spring Security entry points/handlers that run before the DispatcherServlet (and therefore
 * outside the reach of {@code @RestControllerAdvice} and the framework's own ObjectMapper bean).
 */
public final class ApiErrorWriter {

    private ApiErrorWriter() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","fieldErrors":[]}""".formatted(
                Instant.now(), status.value(), status.getReasonPhrase(), escape(message), escape(path));
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
