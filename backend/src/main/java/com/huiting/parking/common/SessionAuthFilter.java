package com.huiting.parking.common;

import com.huiting.parking.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SessionAuthFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/health", "/api/common/districts", "/api/auth/login", "/api/auth/register",
            "/api/auth/send-sms", "/api/auth/reset-password", "/api/admin/login"
    );
    private final SessionService sessions;

    public SessionAuthFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || PUBLIC_PATHS.contains(path) || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        var user = sessions.resolve(request.getHeader("Authorization"));
        if (user.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\",\"data\":null}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        request.setAttribute("authUserId", user.get().id);
        chain.doFilter(request, response);
    }
}
