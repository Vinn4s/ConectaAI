package com.ConectaAI.demo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Applies the initial administrative access boundary without changing public routes.
 * A future identity provider can replace this filter behind the same /admin boundary.
 */
@Component
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final String configuredToken;

    public AdminAuthenticationFilter(@Value("${admin.security.token:}") String configuredToken) {
        this.configuredToken = configuredToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.equals("/admin") && !path.startsWith("/admin/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // The browser never sends the credential on a CORS preflight. The subsequent
        // request still goes through this filter and must carry the admin token.
        if (CorsUtils.isPreFlightRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(configuredToken)) {
            writeError(
                response,
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "A proteção administrativa não está configurada."
            );
            return;
        }

        String suppliedToken = request.getHeader(ADMIN_TOKEN_HEADER);
        if (!tokenMatches(suppliedToken)) {
            response.setHeader("WWW-Authenticate", "AdminToken");
            writeError(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Acesso administrativo não autorizado."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean tokenMatches(String suppliedToken) {
        if (!StringUtils.hasText(suppliedToken)) {
            return false;
        }

        return MessageDigest.isEqual(
            configuredToken.getBytes(StandardCharsets.UTF_8),
            suppliedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"erro\":\"" + message + "\"}");
    }
}
