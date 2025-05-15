package com.example.moneytransferservice.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ApiLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger("api-logger");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Skip logging for non-API endpoints or health checks
        String path = req.getRequestURI();
        if (!path.startsWith("/api") || path.contains("/health")) {
            chain.doFilter(request, response);
            return;
        }

        // Simple request logging (just the essentials)
        log.info("API Request: {} {}", req.getMethod(), path);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            // Log response with timing information
            log.info("API Response: {} {} - Status: {} - Duration: {} ms",
                    req.getMethod(), path, res.getStatus(), System.currentTimeMillis() - startTime);
        }
    }
}
