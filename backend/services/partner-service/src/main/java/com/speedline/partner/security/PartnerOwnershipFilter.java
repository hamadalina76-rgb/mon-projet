package com.speedline.partner.security;

import com.speedline.partner.domain.Partner;
import com.speedline.partner.repository.PartnerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that enforces partner ownership: only the partner owner (userId match) or ADMIN can modify a partner.
 * Reads X-User-Id and X-User-Role from headers (set by api-gateway after JWT validation).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PartnerOwnershipFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Pattern PARTNER_ID_PATH = Pattern.compile("^/partners/(\\d+)(/.*)?$");

    private final PartnerRepository partnerRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only check ownership for modifying operations on /partners/{id} or /partners/{id}/...
        if (!requiresOwnershipCheck(method, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long partnerId = extractPartnerId(path);
        if (partnerId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String roleHeader = request.getHeader(HEADER_USER_ROLE);

        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.warn("Ownership check failed: missing X-User-Id for {} {}", method, path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing authentication context (X-User-Id)\"}");
            return;
        }

        Optional<Partner> partnerOpt = partnerRepository.findById(partnerId);
        if (partnerOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Partner not found with id: " + partnerId + "\"}");
            return;
        }

        Partner partner = partnerOpt.get();
        long requestedUserId = Long.parseLong(userIdHeader.trim());

        if (ROLE_ADMIN.equalsIgnoreCase(roleHeader != null ? roleHeader.trim() : "")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (partner.getUserId() != null && partner.getUserId().equals(requestedUserId)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Ownership check failed: user {} attempted to modify partner {} (owner: {})",
                requestedUserId, partnerId, partner.getUserId());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden: you can only modify your own partner data\"}");
    }

    private boolean requiresOwnershipCheck(String method, String path) {
        if (!path.startsWith("/partners/")) return false;
        // POST /partners/internal is called by auth-service, no user context
        if ("POST".equals(method) && (path.equals("/partners/internal") || path.startsWith("/partners/internal/")))
            return false;
        // GET: only /partners/{id}/staff requires ownership (owner or admin)
        if ("GET".equals(method)) {
            return path.matches("^/partners/\\d+/staff$");
        }
        // PUT, PATCH, POST on /partners/{id} or /partners/{id}/...
        return ("PUT".equals(method) || "PATCH".equals(method) || "POST".equals(method))
                && PARTNER_ID_PATH.matcher(path).matches();
    }

    private Long extractPartnerId(String path) {
        Matcher m = PARTNER_ID_PATH.matcher(path);
        if (!m.matches()) return null;
        try {
            return Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
