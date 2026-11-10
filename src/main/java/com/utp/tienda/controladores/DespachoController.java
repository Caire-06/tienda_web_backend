package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.DespachoLoteService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.CrearDespachoLoteDTO;
import com.utp.tienda.vistas.dto.DespachoLoteDTO;
import com.utp.tienda.vistas.dto.PedidoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de lotes de despacho y manifiestos courier.
 * Conecta las operaciones de Almacén con la supervisión del Administrador.
 */
@RestController
@RequestMapping("/api/despachos")
@CrossOrigin(origins = "*")
public class DespachoController {

    private final DespachoLoteService despachoLoteService;
    private final SesionService sesionService;

    public DespachoController(DespachoLoteService despachoLoteService, SesionService sesionService) {
        this.despachoLoteService = despachoLoteService;
        this.sesionService = sesionService;
    }

    @GetMapping
    public ResponseEntity<List<DespachoLoteDTO>> listarLotes() {
        return ResponseEntity.ok(despachoLoteService.listarLotes());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoDTO>> listarPendientes() {
        return ResponseEntity.ok(despachoLoteService.listarPedidosPendientesDespacho());
    }

    @PostMapping
    public ResponseEntity<?> crearLote(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @Valid @RequestBody CrearDespachoLoteDTO body) {
        Integer usuarioId = sesionService.identificar(auth);
        try {
            DespachoLoteDTO creado = despachoLoteService.crearLote(usuarioId, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<?> completarLote(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(despachoLoteService.marcarLoteCompletado(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
