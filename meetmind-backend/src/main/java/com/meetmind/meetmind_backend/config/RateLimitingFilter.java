package com.meetmind.meetmind_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitingFilter implements Filter {

    private final RateLimiterService rateLimiterService;
    private final int authLimit;
    private final int authWindowSeconds;

    public RateLimitingFilter(
            RateLimiterService rateLimiterService,
            @Value("${ratelimit.auth.limit:5}") int authLimit,
            @Value("${ratelimit.auth.window-seconds:60}") int authWindowSeconds
    ) {
        this.rateLimiterService = rateLimiterService;
        this.authLimit = authLimit;
        this.authWindowSeconds = authWindowSeconds;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String path = httpRequest.getRequestURI();
            String clientIp = getClientIp(httpRequest);
            
            boolean allowed = true;
            if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
                allowed = rateLimiterService.isAllowed(clientIp, "auth", authLimit, authWindowSeconds);
            } else if (path.startsWith("/api/meetings") && httpRequest.getMethod().equals("POST")) {
                allowed = rateLimiterService.isAllowed(clientIp, "meeting_create", 10, 60);
            } else if (path.startsWith("/api/chat") && httpRequest.getMethod().equals("POST")) {
                allowed = rateLimiterService.isAllowed(clientIp, "chat_send", 60, 60);
            }
            
            if (!allowed) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
