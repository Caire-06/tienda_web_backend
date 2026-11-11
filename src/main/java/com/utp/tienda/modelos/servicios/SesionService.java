package com.utp.tienda.modelos.servicios;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sesiones temporales para la fase académica. En producción usar sesión persistente/JWT. */
@Service
public class SesionService {
    private record Sesion(Integer usuarioId, Instant caducidad) {}
    private final ConcurrentHashMap<String, Sesion> sesiones = new ConcurrentHashMap<>();

    public String crear(Integer usuarioId) {
        String token = "TK-" + UUID.randomUUID();
        sesiones.put(token, new Sesion(usuarioId, Instant.now().plus(2, ChronoUnit.HOURS)));
        return token;
    }

    public void revocar(String autorizacion) {
        if (autorizacion != null && autorizacion.startsWith("Bearer ")) {
            sesiones.remove(autorizacion.substring(7).trim());
        }
    }

    /** Revoca todas las sesiones activas pertenecientes a un usuario (ej. tras restablecer contraseña). */
    public void revocarPorUsuario(Integer usuarioId) {
        if (usuarioId != null) {
            sesiones.entrySet().removeIf(entry -> usuarioId.equals(entry.getValue().usuarioId()));
        }
    }

    public Integer identificar(String autorizacion) {
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) return null;
        String token = autorizacion.substring(7).trim();
        Sesion sesion = sesiones.get(token);
        if (sesion == null) return null;
        if (!Instant.now().isBefore(sesion.caducidad())) {
            sesiones.remove(token);
            return null;
        }
        return sesion.usuarioId();
    }
}
