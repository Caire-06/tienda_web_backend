package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.KardexService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.KardexMovimientoDTO;
import com.utp.tienda.vistas.dto.KardexResumenDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kardex")
@CrossOrigin(origins = "*")
public class KardexController {
    private final KardexService kardex;
    private final SesionService sesiones;

    public KardexController(KardexService kardex, SesionService sesiones) {
        this.kardex = kardex;
        this.sesiones = sesiones;
    }

    /** GET /api/kardex/movimientos -> movimientos de entrada/salida (Kardex) con saldo. */
    @GetMapping("/movimientos")
    public ResponseEntity<?> movimientos(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        List<KardexMovimientoDTO> movimientos = kardex.movimientos(id);
        return ResponseEntity.ok(movimientos);
    }

    /** GET /api/kardex/resumen -> resumen del Kardex para el dashboard del almacén. */
    @GetMapping("/resumen")
    public ResponseEntity<?> resumen(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        KardexResumenDTO resumen = kardex.resumen();
        return ResponseEntity.ok(resumen);
    }
}
