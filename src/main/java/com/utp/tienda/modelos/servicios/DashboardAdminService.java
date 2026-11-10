package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.Pedido;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.DashboardKpisDTO;
import com.utp.tienda.vistas.dto.PedidoDTO;
import com.utp.tienda.vistas.dto.ProductoStockCriticoDTO;
import com.utp.tienda.vistas.dto.ProductoTopDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio de métricas y KPIs calculados en Backend para el Dashboard de Administración
 * Cumple con la regla de centralizar cálculos transaccionales en servidor.
 */
@Service
public class DashboardAdminService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;

    public DashboardAdminService(PedidoRepository pedidoRepository,
                                 ProductoRepository productoRepository,
                                 UsuarioRepository usuarioRepository,
                                 PedidoService pedidoService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
    }

    @Transactional(readOnly = true)
    public DashboardKpisDTO obtenerKpis() {
        LocalDate hoy = LocalDate.now();
        String hoyPrefijo = hoy.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String mesPrefijo = hoy.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaDesc();
        List<Producto> productos = productoRepository.findAll();
        List<Usuario> usuarios = usuarioRepository.findAll();

        BigDecimal ventasHoy = BigDecimal.ZERO;
        BigDecimal ventasMes = BigDecimal.ZERO;
        BigDecimal totalHistorico = BigDecimal.ZERO;

        for (Pedido p : pedidos) {
            BigDecimal total = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
            totalHistorico = totalHistorico.add(total);

            java.time.LocalDateTime f = p.getFecha();
            if (f != null) {
                if (f.toLocalDate().isEqual(hoy)) {
                    ventasHoy = ventasHoy.add(total);
                }
                if (f.getYear() == hoy.getYear() && f.getMonth() == hoy.getMonth()) {
                    ventasMes = ventasMes.add(total);
                }
            }
        }

        long totalPedidos = pedidos.size();
        long totalProductos = productos.size();
        long totalUsuarios = usuarios.size();

        long usuariosActivos = usuarios.stream()
                .filter(u -> !"Inactivo".equalsIgnoreCase(u.getEstado()) && !Boolean.TRUE.equals(u.getBloqueado()))
                .count();
        long usuariosInactivos = totalUsuarios - usuariosActivos;

        // Stock Crítico (< 10)
        List<ProductoStockCriticoDTO> criticos = productos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()) && p.getStock() != null && p.getStock() < 10)
                .sorted(Comparator.comparing(Producto::getStock))
                .map(p -> new ProductoStockCriticoDTO(
                        p.getIdProducto(),
                        p.getNombre(),
                        p.getCategoria(),
                        p.getStock(),
                        p.getPrecio()
                ))
                .toList();

        long stockCriticoCount = criticos.size();

        long pedidosPendientesDespacho = pedidos.stream()
                .filter(p -> p.getDespachoLote() == null)
                .filter(p -> !"Cancelado".equalsIgnoreCase(p.getEstado()) && !"Entregado".equalsIgnoreCase(p.getEstado()))
                .count();

        // Top 5 más vendidos
        int maxVendidos = productos.stream()
                .mapToInt(p -> p.getVendidos() != null ? p.getVendidos() : 0)
                .max()
                .orElse(1);
        if (maxVendidos == 0) maxVendidos = 1;

        final int divisorVendidos = maxVendidos;
        List<ProductoTopDTO> topProductos = productos.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getVendidos() != null ? b.getVendidos() : 0,
                        a.getVendidos() != null ? a.getVendidos() : 0
                ))
                .limit(5)
                .map(p -> {
                    int v = p.getVendidos() != null ? p.getVendidos() : 0;
                    double pct = (double) v / divisorVendidos * 100.0;
                    return new ProductoTopDTO(
                            p.getIdProducto(),
                            p.getNombre(),
                            p.getCategoria(),
                            v,
                            p.getPrecio(),
                            Math.round(pct * 10.0) / 10.0
                    );
                })
                .toList();

        // Últimas ventas (5)
        List<PedidoDTO> ultimasVentas = pedidoService.listarTodos().stream()
                .limit(5)
                .toList();

        return new DashboardKpisDTO(
                ventasHoy,
                ventasMes,
                totalHistorico,
                totalPedidos,
                totalProductos,
                totalUsuarios,
                usuariosActivos,
                usuariosInactivos,
                stockCriticoCount,
                pedidosPendientesDespacho,
                criticos,
                topProductos,
                ultimasVentas
        );
    }
}
