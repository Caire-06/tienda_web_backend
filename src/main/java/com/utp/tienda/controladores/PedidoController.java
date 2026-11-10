package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.PedidoService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.CrearPedidoDTO;
import com.utp.tienda.vistas.dto.PedidoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {
    private final PedidoService pedidos;
    private final SesionService sesiones;

    public PedidoController(PedidoService pedidos, SesionService sesiones) {
        this.pedidos = pedidos;
        this.sesiones = sesiones;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @Valid @RequestBody CrearPedidoDTO dto) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        try {
            PedidoDTO creado = pedidos.crear(id, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mis-compras")
    public ResponseEntity<?> misCompras(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        List<PedidoDTO> lista = pedidos.misCompras(id);
        return ResponseEntity.ok(lista);
    }

    @GetMapping
    public ResponseEntity<?> listarTodos(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        return ResponseEntity.ok(pedidos.listarTodos());
    }

    @PutMapping("/{numero}/estado")
    public ResponseEntity<?> actualizarEstado(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable String numero,
                                              @RequestBody Map<String, String> body) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado requerido"));
        }
        try {
            PedidoDTO actualizado = pedidos.actualizarEstado(id, numero, nuevoEstado);
            return ResponseEntity.ok(actualizado);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
