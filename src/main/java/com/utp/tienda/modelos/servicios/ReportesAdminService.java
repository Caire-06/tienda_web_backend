package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Reportería Estratégica (Etapa 7 del ciclo de desarrollo)
 * Genera métricas analíticas para Usuarios, Ventas e Inventario Valorizado.
 */
@Service
public class ReportesAdminService {

    private final UsuarioRepository usuarioRepository;
    private final DatosPersonalesRepository datosPersonalesRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public ReportesAdminService(UsuarioRepository usuarioRepository,
                                DatosPersonalesRepository datosPersonalesRepository,
                                PedidoRepository pedidoRepository,
                                ProductoRepository productoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.datosPersonalesRepository = datosPersonalesRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporte(String tipo) {
        String t = tipo.toLowerCase().trim();
        return switch (t) {
            case "usuarios" -> generarReporteUsuarios();
            case "ventas" -> generarReporteVentas();
            case "inventario" -> generarReporteInventario();
            default -> Map.of(
                    "usuarios", generarReporteUsuarios(),
                    "ventas", generarReporteVentas(),
                    "inventario", generarReporteInventario()
            );
        };
    }

    private Map<String, Object> generarReporteUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        long total = usuarios.size();
        long activos = usuarios.stream().filter(u -> !"Inactivo".equalsIgnoreCase(u.getEstado()) && !Boolean.TRUE.equals(u.getBloqueado())).count();
        long inactivos = total - activos;
        long bloqueados = usuarios.stream().filter(u -> Boolean.TRUE.equals(u.getBloqueado())).count();
        long conIntentosFallidos = usuarios.stream().filter(u -> u.getIntentosFallidos() != null && u.getIntentosFallidos() > 0).count();

        List<Map<String, Object>> filas = usuarios.stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getIdUsuario());
            row.put("username", u.getNombreUsuario());
            row.put("rol", u.getRol() != null ? u.getRol().getNombreRol() : "CLIENTE");
            row.put("estado", u.getEstado());
            row.put("bloqueado", Boolean.TRUE.equals(u.getBloqueado()));
            row.put("intentosFallidos", u.getIntentosFallidos() != null ? u.getIntentosFallidos() : 0);
            row.put("fechaCreacion", u.getFechaCreacion() != null ? u.getFechaCreacion().toString() : "—");
            row.put("ultimoAcceso", u.getUltimoAcceso() != null ? u.getUltimoAcceso().toString() : "—");
            return row;
        }).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("tipo", "usuarios");
        resultado.put("total", total);
        resultado.put("activos", activos);
        resultado.put("inactivos", inactivos);
        resultado.put("bloqueados", bloqueados);
        resultado.put("conIntentosFallidos", conIntentosFallidos);
        resultado.put("filas", filas);
        return resultado;
    }

    private Map<String, Object> generarReporteVentas() {
        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaDesc();
        long totalPedidos = pedidos.size();
        BigDecimal totalVentas = pedidos.stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketPromedio = totalPedidos > 0
                ? totalVentas.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Agrupación de ventas por día/fecha (yyyy-MM-dd)
        Map<String, BigDecimal> porDia = new TreeMap<>();
        for (Pedido p : pedidos) {
            String f = p.getFecha() != null ? p.getFecha().toLocalDate().toString() : "Sin fecha";
            porDia.merge(f, p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO, BigDecimal::add);
        }

        // Agrupación por cliente
        Map<String, BigDecimal> porCliente = new HashMap<>();
        for (Pedido p : pedidos) {
            String c = p.getUsuario() != null ? p.getUsuario().getNombreUsuario() : "Invitado";
            porCliente.merge(c, p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO, BigDecimal::add);
        }

        List<Map<String, Object>> filas = pedidos.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("numero", p.getNumero());
            row.put("fecha", p.getFecha() != null ? p.getFecha().toString() : "—");
            row.put("cliente", p.getUsuario() != null ? p.getUsuario().getNombreUsuario() : "—");
            row.put("total", p.getTotal());
            row.put("estado", p.getEstado());
            row.put("metodoPago", p.getMetodoPago());
            row.put("tipoEntrega", p.getTipoEntrega());
            ComprobantePedido comp = p.getComprobante();
            String tipoComp = comp != null && comp.getTipo() != null ? comp.getTipo() : "BOLETA";
            String serie = "FACTURA".equalsIgnoreCase(tipoComp) ? "F001-" : "B001-";
            row.put("tipoComprobante", tipoComp);
            row.put("numeroComprobante", serie + p.getNumero());
            return row;
        }).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("tipo", "ventas");
        resultado.put("totalPedidos", totalPedidos);
        resultado.put("totalVentas", totalVentas);
        resultado.put("ticketPromedio", ticketPromedio);
        resultado.put("ventasPorDia", porDia);
        resultado.put("ventasPorCliente", porCliente);
        resultado.put("filas", filas);
        return resultado;
    }

    private Map<String, Object> generarReporteInventario() {
        List<Producto> productos = productoRepository.findAll();
        long totalProductos = productos.size();
        long stockTotal = productos.stream().mapToLong(p -> p.getStock() != null ? p.getStock() : 0).sum();

        BigDecimal stockValorizado = productos.stream()
                .map(p -> {
                    BigDecimal pr = p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO;
                    int st = p.getStock() != null ? p.getStock() : 0;
                    return pr.multiply(BigDecimal.valueOf(st));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> filas = productos.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getIdProducto());
            row.put("nombre", p.getNombre());
            row.put("categoria", p.getCategoria());
            row.put("precio", p.getPrecio());
            row.put("stock", p.getStock());
            row.put("vendidos", p.getVendidos());
            row.put("activo", p.getActivo());
            BigDecimal val = (p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(p.getStock() != null ? p.getStock() : 0));
            row.put("valorizado", val);
            row.put("rotacionCritica", p.getStock() != null && p.getStock() < 10);
            return row;
        }).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("tipo", "inventario");
        resultado.put("totalProductos", totalProductos);
        resultado.put("stockTotal", stockTotal);
        resultado.put("stockValorizado", stockValorizado);
        resultado.put("filas", filas);
        return resultado;
    }
}
