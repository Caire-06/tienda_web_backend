package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.ReportesAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para el módulo de Reportería Estratégica (Etapa 7)
 * Permite consultas parametrizadas para usuarios, ventas e inventario valorizado.
 */
@RestController
@RequestMapping("/api/admin/reportes")
@CrossOrigin(origins = "*")
public class AdminReporteController {

    private final ReportesAdminService reportesAdminService;

    public AdminReporteController(ReportesAdminService reportesAdminService) {
        this.reportesAdminService = reportesAdminService;
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<Map<String, Object>> obtenerReporte(@PathVariable String tipo) {
        return ResponseEntity.ok(reportesAdminService.generarReporte(tipo));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerTodosLosReportes() {
        return ResponseEntity.ok(reportesAdminService.generarReporte("todos"));
    }
}

