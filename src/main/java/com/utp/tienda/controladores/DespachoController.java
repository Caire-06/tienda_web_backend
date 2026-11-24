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
public class DespachoController {

    private final DespachoLoteService despachoLoteService;
    private final SesionService sesionService;

    public DespachoController(DespachoLoteService despachoLoteService, SesionService sesionService) {
        this.despachoLoteService = despachoLoteService;
        this.sesionService = sesionService;
    }

    @GetMapping
    public ResponseEntity<?> listarLotes(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        if (page == null) {
            return ResponseEntity.ok(despachoLoteService.listarLotes());
        }
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, Math.max(1, Math.min(size, 100)));
        return ResponseEntity.ok(com.utp.tienda.vistas.dto.PaginaDTO.de(despachoLoteService.listarLotesPaginado(pageable)));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        if (page == null) {
            return ResponseEntity.ok(despachoLoteService.listarPedidosPendientesDespacho());
        }
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, Math.max(1, Math.min(size, 100)));
        return ResponseEntity.ok(com.utp.tienda.vistas.dto.PaginaDTO.de(despachoLoteService.listarPedidosPendientesDespachoPaginado(pageable)));
    }

    @PostMapping
    public ResponseEntity<?> crearLote(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @Valid @RequestBody CrearDespachoLoteDTO body) {
        Integer usuarioId = sesionService.identificar(auth);
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        }
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

