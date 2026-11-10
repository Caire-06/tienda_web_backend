package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.PerfilService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.ActualizarPerfilDTO;
import com.utp.tienda.vistas.dto.CambiarPasswordDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {
    private final PerfilService perfiles;
    private final SesionService sesiones;

    public PerfilController(PerfilService perfiles, SesionService sesiones) {
        this.perfiles = perfiles;
        this.sesiones = sesiones;
    }

    @GetMapping
    public ResponseEntity<?> consultar(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida o vencida"));
        try { return ResponseEntity.ok(perfiles.consultar(id)); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }

    @PutMapping
    public ResponseEntity<?> actualizar(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @Valid @RequestBody ActualizarPerfilDTO request) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida o vencida"));
        try { return ResponseEntity.ok(perfiles.actualizar(id, request)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @Valid @RequestBody CambiarPasswordDTO request) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida o vencida"));
        try {
            perfiles.cambiarPassword(id, request);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
