package com.utp.tienda.controladores.filtros;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Patrón Estructural: Decorator en Middlewares
 * Decora dinámicamente las solicitudes y respuestas HTTP añadiendo cabeceras
 * de seguridad, CORS controlado por entorno y metadatos de auditoría.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeaderDecoratorFilter implements Filter {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:4173}")
    private String corsAllowedOriginsRaw;

    private List<String> origenesPermitidos() {
        if (corsAllowedOriginsRaw == null || corsAllowedOriginsRaw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(corsAllowedOriginsRaw.split(","))
                .map(String::trim)
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .filter(s -> !s.isBlank())
                .toList();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        RequestAuditDecorator decoratedRequest = new RequestAuditDecorator(httpRequest);

        // Cabeceras de seguridad
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Política CORS basada en CORS_ALLOWED_ORIGINS (Render / Local)
        String origin = httpRequest.getHeader("Origin");
        List<String> permitidos = origenesPermitidos();
        boolean permiteTodos = permitidos.contains("*");
        String originNormalizado = origin != null && origin.endsWith("/")
                ? origin.substring(0, origin.length() - 1)
                : origin;
        boolean originValido = originNormalizado != null && (permiteTodos || permitidos.contains(originNormalizado));

        if (originValido) {
            httpResponse.setHeader("Access-Control-Allow-Origin", permiteTodos ? "*" : originNormalizado);
            httpResponse.setHeader("Vary", "Origin");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        }

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(origin != null && !originValido
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(decoratedRequest, httpResponse);
    }

    /**
     * Decorador de HttpServletRequest
     */
    public static class RequestAuditDecorator extends HttpServletRequestWrapper {
        public RequestAuditDecorator(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if ("X-Audited-By".equalsIgnoreCase(name)) {
                return "UTP-Security-Filter";
            }
            return super.getHeader(name);
        }
    }
}
