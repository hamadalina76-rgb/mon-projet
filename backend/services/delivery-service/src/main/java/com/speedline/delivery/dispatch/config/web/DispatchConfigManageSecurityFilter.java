package com.speedline.delivery.dispatch.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * DISP-205: coarse admin boundary aligned with admin-panel {@code delivery:manage} UX.
 * Downstream receives gateway-injected {@code X-User-Role}; SUPER_ADMIN and ADMIN are allowed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class DispatchConfigManageSecurityFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.contains("/dispatch/dispatch-config");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String role = normalize(request.getHeader("X-User-Role"));
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "dispatch config requires admin role");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        return v.startsWith("ROLE_") ? v.substring(5) : v;
    }
}
