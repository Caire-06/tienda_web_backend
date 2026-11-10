package com.utp.tienda.controladores.filtros;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Patrón Estructural: Decorator en Middlewares
 * Decora dinámicamente las solicitudes y respuestas HTTP añadiendo cabeceras
 * de seguridad y metadatos de auditoría sin modificar la clase original.
 */
@Component
public class SecurityHeaderDecoratorFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Decoración de la Petición (Patrón Decorator)
        RequestAuditDecorator decoratedRequest = new RequestAuditDecorator(httpRequest);

        // Decoración de la Respuesta con Cabeceras de Seguridad y CORS
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
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

