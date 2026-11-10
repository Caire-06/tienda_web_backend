package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardKpisDTO(
        BigDecimal ventasHoy,
        BigDecimal ventasMes,
        BigDecimal totalHistorico,
        Long totalPedidos,
        Long totalProductos,
        Long totalUsuarios,
        Long totalUsuariosActivos,
        Long totalUsuariosInactivos,
        Long stockCriticoCount,
        List<ProductoStockCriticoDTO> productosStockCritico,
        List<ProductoTopDTO> topProductos,
        List<PedidoDTO> ultimasVentas
) {}
