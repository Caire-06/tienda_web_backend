package com.utp.tienda.controladores.filtros;

import com.utp.tienda.modelos.servicios.ControlTraficoService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Filtro de control de tasa (Rate Limiting) sobre endpoints públicos y de alto costo.
 * Se ejecuta justo después de SecurityHeaderDecoratorFilter para conservar cabeceras CORS en HTTP 429.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitDecoratorFilter implements Filter {

    private record ReglaLimite(
            String idRegla,
            String metodo,
            String prefijoRuta,
            int maxSolicitudes,
            long ventanaSegundos,
            boolean usarTokenSiExiste
    ) {}

    private static final List<ReglaLimite> REGLAS = List.of(
            new ReglaLimite("auth-registro", "POST", "/api/auth/registro", 4, 600, false),
            new ReglaLimite("auth-registro", "POST", "/api/auth/register", 4, 600, false),
            new ReglaLimite("auth-recuperar", "POST", "/api/auth/recuperar-password", 4, 600, false),
            new ReglaLimite("auth-reenviar", "POST", "/api/auth/reenviar-codigo", 6, 600, false),
            new ReglaLimite("auth-login", "POST", "/api/auth/login", 15, 300, false),
            new ReglaLimite("auth-otp", "POST", "/api/auth/verificar-correo", 15, 300, false),
            new ReglaLimite("auth-otp", "POST", "/api/auth/verificar-2fa", 15, 300, false),
            new ReglaLimite("auth-otp", "POST", "/api/auth/restablecer-password", 12, 300, false),
            new ReglaLimite("fiscal-min", "GET", "/api/consultas/", 12, 60, false),
            new ReglaLimite("fiscal-hora", "GET", "/api/consultas/", 40, 3600, false),
            new ReglaLimite("upload-comprobante", "POST", "/api/pedidos/comprobante-pago", 10, 600, true),
            new ReglaLimite("upload-producto", "POST", "/api/productos/imagen", 25, 600, true),
            new ReglaLimite("crear-pedido", "POST", "/api/pedidos", 10, 600, false)
    );

    private final ControlTraficoService controlTraficoService;

    public RateLimitDecoratorFilter(ControlTraficoService controlTraficoService) {
        this.controlTraficoService = controlTraficoService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String metodo = req.getMethod();
        if ("OPTIONS".equalsIgnoreCase(metodo)) {
            chain.doFilter(request, response);
            return;
        }

        String ruta = req.getRequestURI();
        if (ruta == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = controlTraficoService.obtenerIpCliente(req);
        for (ReglaLimite regla : REGLAS) {
            if (!regla.metodo().equalsIgnoreCase(metodo)) continue;
            if (!coincideRuta(regla, ruta)) continue;

            String clienteKey = ip;
            if (regla.usarTokenSiExiste()) {
                String auth = req.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ") && auth.length() > 15) {
                    clienteKey = "tk:" + Math.abs(auth.hashCode());
                }
            }

            ControlTraficoService.ResultadoLimite resultado = controlTraficoService.intentarConsumir(
                    regla.idRegla(), clienteKey, regla.maxSolicitudes(), regla.ventanaSegundos());

            if (!resultado.permitido()) {
                responder429(res, resultado.segundosEspera());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static boolean coincideRuta(ReglaLimite regla, String ruta) {
        if ("crear-pedido".equals(regla.idRegla())) {
            return "/api/pedidos".equals(ruta) || "/api/pedidos/".equals(ruta);
        }
        return regla.prefijoRuta().endsWith("/")
                ? ruta.startsWith(regla.prefijoRuta())
                : ruta.equals(regla.prefijoRuta());
    }

    private static void responder429(HttpServletResponse res, long segundosEspera) throws IOException {
        res.setStatus(429);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/json; charset=UTF-8");
        res.setHeader("Retry-After", String.valueOf(segundosEspera));
        String msg = "Demasiadas solicitudes en poco tiempo. Espera " + segundosEspera + " segundo(s) antes de volver a intentar.";
        String json = "{\"exitoso\":false,\"error\":\"" + msg + "\",\"mensaje\":\"" + msg
                + "\",\"segundosEspera\":" + segundosEspera + "}";
        res.getWriter().write(json);
    }
}

