package com.angular.backend.config;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

        long startTime = System.currentTimeMillis();
        chain.doFilter(requestWrapper, responseWrapper);
        long timeTaken = System.currentTimeMillis() - startTime;

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());

        log.info(
                "REQUEST: method={}, uri={}, body={}, timeTaken={}",
                requestWrapper.getMethod(), requestWrapper.getRequestURI(), requestBody, timeTaken);
        logHeaders(requestWrapper);

        log.info("RESPONSE: status={}, body={}", responseWrapper.getStatus(), responseBody);

        responseWrapper.copyBodyToResponse();
    }

    private void logHeaders(ContentCachingRequestWrapper request) {
        StringBuilder headers = new StringBuilder();
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            headers.append(headerName).append(": ").append(request.getHeader(headerName)).append("; ");
        });
        if (headers.length() > 0) {
            log.info("Request Headers: [{}]", headers.substring(0, headers.length() - 2));
        }
    }

    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            String value = new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
            // Sanitize to prevent log injection, removing new lines
            return value.replaceAll("[\n\r\t]", "_");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to read request/response body", e);
        }
        return "";
    }
}
