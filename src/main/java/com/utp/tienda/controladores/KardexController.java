
package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.KardexService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.KardexMovimientoDTO;
import com.utp.tienda.vistas.dto.KardexResumenDTO;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kardex")
public class KardexController {

    private final KardexService kardex;
    private final SesionService sesiones;

    public KardexController(
            KardexService kardex,
            SesionService sesiones
    ) {
        this.kardex = kardex;
        this.sesiones = sesiones;
    }

    /**
     * GET /api/kardex/movimientos
     *
     * Permite consultar los movimientos del Kardex
     * con filtros opcionales y paginación.
     */
    @GetMapping("/movimientos")
    public ResponseEntity<?> movimientos(

            @RequestParam(value = "page", required = false) Integer page,

            @RequestParam(value = "size", defaultValue = "10") Integer size,

            @RequestParam(value = "productoId", required = false) Integer productoId,

            @RequestParam(value = "desde", required = false) LocalDate desde,

            @RequestParam(value = "hasta", required = false) LocalDate hasta,

            @RequestHeader(value = "Authorization", required = false) String auth
    ) {

        Integer id = sesiones.identificar(auth);

        if (id == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión inválida"));
        }

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fecha inicial no puede ser posterior a la fecha final"));
        }

        List<KardexMovimientoDTO> movimientos = kardex.movimientos(
                id,
                productoId,
                desde,
                hasta
        );

        // Sin paginación: devuelve todos los resultados filtrados.
        if (page == null) {
            return ResponseEntity.ok(movimientos);
        }

        int paginaSegura = Math.max(0, page);
        int tamanoSeguro = Math.max(1, Math.min(size, 100));

        Pageable pageable = PageRequest.of(
                paginaSegura,
                tamanoSeguro
        );

        int total = movimientos.size();

        int inicio = (int) pageable.getOffset();

        int fin = Math.min(
                inicio + pageable.getPageSize(),
                total
        );

        List<KardexMovimientoDTO> contenido = inicio >= total
                ? List.of()
                : movimientos.subList(inicio, fin);

        int totalPaginas = (int) Math.ceil(
                (double) total / pageable.getPageSize()
        );

        return ResponseEntity.ok(
                new com.utp.tienda.vistas.dto.PaginaDTO<>(
                        contenido,
                        total,
                        Math.max(1, totalPaginas),
                        pageable.getPageNumber(),
                        pageable.getPageSize()
                )
        );
    }

    /**
     * GET /api/kardex/resumen
     *
     * Resumen general del Kardex para el dashboard.
     */
    @GetMapping("/resumen")
    public ResponseEntity<?> resumen(
            @RequestHeader(value = "Authorization", required = false) String auth
    ) {

        Integer id = sesiones.identificar(auth);

        if (id == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión inválida"));
        }

        KardexResumenDTO resumen = kardex.resumen();

        return ResponseEntity.ok(resumen);
    }
}
