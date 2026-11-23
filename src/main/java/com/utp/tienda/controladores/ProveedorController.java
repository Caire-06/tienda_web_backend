package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.ProveedorService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.CrearProveedorDTO;
import com.utp.tienda.modelos.entidades.Proveedor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {
    private final ProveedorService proveedores;
    private final SesionService sesiones;

    public ProveedorController(ProveedorService proveedores, SesionService sesiones) {
        this.proveedores = proveedores;
        this.sesiones = sesiones;
    }

    private Integer usuarioIdO401(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        return sesiones.identificar(auth);
    }

    private ResponseEntity<?> noAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
    }

    /** GET /api/proveedores -> listar proveedores activos (catálogo para registrar entradas). */
    @GetMapping
    public ResponseEntity<?> listar(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = usuarioIdO401(auth);
        if (id == null) return noAutorizado();
        return ResponseEntity.ok(proveedores.listarActivos());
    }

    /** POST /api/proveedores -> registrar un nuevo proveedor. */
    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth,
                                   @jakarta.validation.Valid @RequestBody CrearProveedorDTO dto) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(proveedores.crear(usuarioId, dto));
        } catch (IllegalArgumentException e) {
            // El service lanza IllegalArgumentException si el rol no está permitido.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
