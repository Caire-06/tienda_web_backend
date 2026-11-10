package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.modelos.servicios.UsuarioService;
import com.utp.tienda.vistas.dto.CrearUsuarioAdminDTO;
import com.utp.tienda.vistas.dto.UsuarioCreadoAdminDTO;
import com.utp.tienda.vistas.dto.UsuarioDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador MVC para el Módulo de Usuarios
 * Patrones aplicados: Repository, Factory Method, Memento
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final SesionService sesionService;

    public UsuarioController(UsuarioService usuarioService, SesionService sesionService) {
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(usuarioService.listarUsuariosDTO());
    }

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody CrearUsuarioAdminDTO body) {
        try {
            UsuarioCreadoAdminDTO creado = usuarioService.crearUsuarioDesdeAdmin(body);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            String nuevoRol = body.get("rol");
            if (nuevoRol == null || nuevoRol.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rol requerido"));
            }
            UsuarioDTO actualizado = usuarioService.cambiarRol(id, nuevoRol);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Boolean bloqueado = body.get("bloqueado") != null ? (Boolean) body.get("bloqueado") : false;
            UsuarioDTO actualizado = usuarioService.cambiarEstadoBloqueo(id, bloqueado);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/baja-logica")
    public ResponseEntity<?> darDeBajaLogica(@PathVariable Integer id) {
        try {
            UsuarioDTO actualizado = usuarioService.darDeBajaLogica(id);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        // En lugar de eliminación física, realiza baja lógica manteniendo integridad y trazabilidad
        try {
            UsuarioDTO actualizado = usuarioService.darDeBajaLogica(id);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
