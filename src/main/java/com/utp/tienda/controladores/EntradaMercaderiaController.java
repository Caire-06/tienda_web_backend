package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.EntradaMercaderiaService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.CrearEntradaDTO;
import com.utp.tienda.vistas.dto.EntradaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entradas")
public class EntradaMercaderiaController {
    private final EntradaMercaderiaService entradas;
    private final SesionService sesiones;

    public EntradaMercaderiaController(EntradaMercaderiaService entradas, SesionService sesiones) {
        this.entradas = entradas;
        this.sesiones = sesiones;
    }

    /** POST /api/entradas -> registrar entrada de mercadería con sus líneas. */
    @PostMapping
    public ResponseEntity<?> registrar(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @Valid @RequestBody CrearEntradaDTO dto) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        try {
            EntradaDTO entrada = entradas.registrarEntrada(id, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(entrada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/entradas/mias -> historial de entradas registradas. */
    @GetMapping("/mis-entradas")
    public ResponseEntity<?> misEntradas(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        if (page == null) {
            List<EntradaDTO> lista = entradas.misEntradas(id);
            return ResponseEntity.ok(lista);
        }
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, Math.max(1, Math.min(size, 100)));
        return ResponseEntity.ok(com.utp.tienda.vistas.dto.PaginaDTO.de(entradas.misEntradasPaginado(id, pageable)));
    }
}
