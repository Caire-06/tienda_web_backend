package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.DashboardAdminService;
import com.utp.tienda.vistas.dto.DashboardKpisDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de métricas de alto nivel para el Dashboard del Perfil Administrador.
 * Implementa el cálculo centralizado en servidor requerido por las normas arquitectónicas.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardAdminService dashboardAdminService;

    public AdminDashboardController(DashboardAdminService dashboardAdminService) {
        this.dashboardAdminService = dashboardAdminService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<DashboardKpisDTO> obtenerKpis() {
        return ResponseEntity.ok(dashboardAdminService.obtenerKpis());
    }
}

