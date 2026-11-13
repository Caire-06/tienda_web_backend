package com.utp.tienda.controladores.filtros;

import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.modelos.servicios.SesionService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Filtro RBAC centralizado (Fail-Closed / Deny-by-default para rutas /api/**).
 * Verifica sesión activa (401) y autorización por rol (403) antes de alcanzar los controladores.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AutorizacionDecoratorFilter implements Filter {

    public enum PoliticaRuta {
        PUBLICO,
        AUTENTICADO,
        ADMIN_O_ALMACEN,
        SOLO_ADMIN,
        BLOQUEADO
    }

    private static final Set<String> ROLES_OPERATIVOS = Set.of("ADMINISTRADOR", "ALMACEN");

    private final SesionService sesionService;
    private final UsuarioRepository usuarioRepository;

    public AutorizacionDecoratorFilter(SesionService sesionService, UsuarioRepository usuarioRepository) {
        this.sesionService = sesionService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String metodo = req.getMethod() != null ? req.getMethod().toUpperCase() : "GET";
        if ("OPTIONS".equals(metodo)) {
            chain.doFilter(request, response);
            return;
        }

        String rutaNormalizada = normalizarRuta(req.getRequestURI());
        PoliticaRuta politica = resolverPolitica(metodo, rutaNormalizada);

        if (politica == PoliticaRuta.BLOQUEADO) {
            responderError(res, HttpServletResponse.SC_FORBIDDEN, "Ruta no permitida.");
            return;
        }
        if (politica == PoliticaRuta.PUBLICO) {
            chain.doFilter(request, response);
            return;
        }

        String auth = req.getHeader("Authorization");
        Integer usuarioId = sesionService.identificar(auth);
        if (usuarioId == null) {
            responderError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Sesión inválida o expirada. Inicia sesión nuevamente.");
            return;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) {
            sesionService.revocar(auth);
            responderError(res, HttpServletResponse.SC_UNAUTHORIZED, "Usuario de sesión no encontrado.");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        if (Boolean.TRUE.equals(usuario.getBloqueado()) || "Inactivo".equalsIgnoreCase(usuario.getEstado())) {
            sesionService.revocar(auth);
            responderError(res, HttpServletResponse.SC_UNAUTHORIZED, "Tu cuenta se encuentra inactiva o bloqueada.");
            return;
        }

        String rol = usuario.getRol() != null && usuario.getRol().getNombreRol() != null
                ? usuario.getRol().getNombreRol().trim().toUpperCase()
                : "CLIENTE";

        if (!rolPermitido(politica, rol)) {
            responderError(res, HttpServletResponse.SC_FORBIDDEN,
                    "No tienes permisos suficientes para acceder a este recurso.");
            return;
        }

        chain.doFilter(request, response);
    }

    public static PoliticaRuta resolverPolitica(String metodo, String ruta) {
        if (ruta == null || ruta.contains("..") || ruta.contains(";")) {
            return PoliticaRuta.BLOQUEADO;
        }
        if (!ruta.startsWith("/api")) {
            return PoliticaRuta.PUBLICO;
        }

        // 1. Endpoints públicos explícitos
        if (coincidePrefijo(ruta, "/api/auth")) {
            return PoliticaRuta.PUBLICO;
        }
        if ("GET".equals(metodo) && coincidePrefijo(ruta, "/api/consultas")) {
            return PoliticaRuta.PUBLICO;
        }
        if ("GET".equals(metodo) && (ruta.equals("/api/productos") || ruta.matches("^/api/productos/[^/]+$"))) {
            return PoliticaRuta.PUBLICO;
        }

        // 2. Endpoints exclusivos de ADMINISTRADOR
        if (coincidePrefijo(ruta, "/api/admin") || coincidePrefijo(ruta, "/api/usuarios")) {
            return PoliticaRuta.SOLO_ADMIN;
        }
        if ("PUT".equals(metodo) && ruta.startsWith("/api/pedidos/") && ruta.endsWith("/pago")) {
            return PoliticaRuta.SOLO_ADMIN;
        }

        // 3. Endpoints operativos de ADMINISTRADOR o ALMACEN
        if (coincidePrefijo(ruta, "/api/despachos")
                || coincidePrefijo(ruta, "/api/entradas")
                || coincidePrefijo(ruta, "/api/kardex")) {
            return PoliticaRuta.ADMIN_O_ALMACEN;
        }
        if ("GET".equals(metodo) && "/api/pedidos".equals(ruta)) {
            return PoliticaRuta.ADMIN_O_ALMACEN;
        }
        if ("PUT".equals(metodo) && ruta.startsWith("/api/pedidos/") && ruta.endsWith("/estado")) {
            return PoliticaRuta.ADMIN_O_ALMACEN;
        }
        if (coincidePrefijo(ruta, "/api/productos") && !"GET".equals(metodo)) {
            return PoliticaRuta.ADMIN_O_ALMACEN;
        }

        // 4. Cualquier otro /api/** requiere usuario autenticado (perfil, mis-compras, crear pedido, comprobante)
        return PoliticaRuta.AUTENTICADO;
    }

    public static boolean rolPermitido(PoliticaRuta politica, String rol) {
        String rolNorm = rol != null ? rol.trim().toUpperCase() : "";
        return switch (politica) {
            case PUBLICO -> true;
            case AUTENTICADO -> !rolNorm.isBlank();
            case ADMIN_O_ALMACEN -> ROLES_OPERATIVOS.contains(rolNorm);
            case SOLO_ADMIN -> "ADMINISTRADOR".equals(rolNorm);
            case BLOQUEADO -> false;
        };
    }

    static String normalizarRuta(String uri) {
        if (uri == null || uri.isBlank()) return "/";
        String limpia = uri.replaceAll("/+", "/");
        if (limpia.length() > 1 && limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }

    private static boolean coincidePrefijo(String ruta, String prefijo) {
        return ruta.equals(prefijo) || ruta.startsWith(prefijo + "/");
    }

    private static void responderError(HttpServletResponse res, int status, String mensaje) throws IOException {
        res.setStatus(status);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/json; charset=UTF-8");
        String json = "{\"exitoso\":false,\"error\":\"" + mensaje + "\",\"mensaje\":\"" + mensaje + "\"}";
        res.getWriter().write(json);
    }
}

