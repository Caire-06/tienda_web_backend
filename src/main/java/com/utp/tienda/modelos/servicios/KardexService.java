package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.DetalleEntrada;
import com.utp.tienda.modelos.entidades.DetallePedido;
import com.utp.tienda.modelos.entidades.Entrada;
import com.utp.tienda.modelos.entidades.Pedido;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.repositorios.EntradaRepository;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.servicios.ProductoService;
import com.utp.tienda.vistas.dto.KardexMovimientoDTO;
import com.utp.tienda.vistas.dto.KardexResumenDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KardexService {
    private final PedidoRepository pedidos;
    private final EntradaRepository entradas;
    private final ProductoRepository productos;
    private final ProductoService productoService;

    public KardexService(PedidoRepository pedidos, EntradaRepository entradas, ProductoRepository productos, ProductoService productoService) {
        this.pedidos = pedidos;
        this.entradas = entradas;
        this.productos = productos;
        this.productoService = productoService;
    }

    /**
     * Devuelve los movimientos de entrada/salida (Kardex) derivados de las tablas 3FN ya existentes:
     * - Ingresos desde: entradas (cabecera) -> detalle_entradas
     * - Salidas desde: pedidos (cabecera) -> detalle_pedidos
     */
    @Transactional(readOnly = true)
    public List<KardexMovimientoDTO> movimientos() {
        List<KardexMovimientoDTO> movimientos = new ArrayList<>();

        // Ingresos: Entrada (cabecera) 1:N DetalleEntrada
        for (Entrada e : entradas.findAll()) {
            for (DetalleEntrada d : e.getDetalles()) {
                movimientos.add(new KardexMovimientoDTO(
                        "E" + e.getIdEntrada() + "-" + d.getIdDetalleEntrada(),
                        e.getFecha() != null ? e.getFecha().toString() : "",
                        "Entrada",
                        d.getProductoId(),
                        d.getNombre(),
                        d.getCantidad(),
                        d.getCostoUnitario(),
                        null,
                        null,
                        "Ingreso de mercadería",
                        e.getUsuario() != null ? e.getUsuario().getNombreUsuario() : ""));
            }
        }

        // Salidas: Pedido (cabecera) 1:N DetallePedido
        for (Pedido p : pedidos.findAll()) {
            for (DetallePedido d : p.getDetalles()) {
                movimientos.add(new KardexMovimientoDTO(
                        "P" + p.getIdPedido() + "-" + d.getIdDetallePedido(),
                        p.getFecha() != null ? p.getFecha().toString() : "",
                        "Salida",
                        d.getProductoId(),
                        d.getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        null,
                        null,
                        "Venta " + p.getNumero(),
                        p.getUsuario() != null ? p.getUsuario().getNombreUsuario() : ""));
            }
        }

        // Calcular stockAnterior / stockNuevo por producto (recorrido inverso sobre las operaciones)
        Map<Integer, List<KardexMovimientoDTO>> porProducto = new LinkedHashMap<>();
        for (KardexMovimientoDTO m : movimientos) {
            porProducto.computeIfAbsent(m.productoId(), k -> new ArrayList<>()).add(m);
        }

        List<KardexMovimientoDTO> resultado = new ArrayList<>();
        for (Map.Entry<Integer, List<KardexMovimientoDTO>> entry : porProducto.entrySet()) {
            List<KardexMovimientoDTO> lista = entry.getValue();
            lista.sort(Comparator.comparing(KardexMovimientoDTO::fecha));
            Producto prod = productos.findById(entry.getKey()).orElse(null);
            int saldo = prod != null && prod.getStock() != null ? prod.getStock() : 0;
            List<KardexMovimientoDTO> ajustadas = new ArrayList<>();
            for (int i = lista.size() - 1; i >= 0; i--) {
                KardexMovimientoDTO m = lista.get(i);
                int delta = "Entrada".equals(m.tipo()) ? m.cantidad() : -m.cantidad();
                int stockNuevo = saldo;
                int stockAnterior = stockNuevo - delta;
                saldo = stockAnterior;
                ajustadas.add(new KardexMovimientoDTO(
                        m.id(), m.fecha(), m.tipo(), m.productoId(), m.producto(), m.cantidad(),
                        m.valorUnitario(), stockAnterior, stockNuevo, m.motivo(), m.usuario()));
            }
            ajustadas.sort(Comparator.comparing(KardexMovimientoDTO::fecha));
            resultado.addAll(ajustadas);
        }

        resultado.sort(Comparator.comparing(KardexMovimientoDTO::fecha).reversed());
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<KardexMovimientoDTO> movimientos(Integer usuarioId) {
        productoService.verificarRolPermitido(usuarioId);
        return movimientos();
    }

    /**
     * Resumen del Kardex para el dashboard del almacén:
     * totales de movimientos, stock y últimos movimientos registrados.
     */
    @Transactional(readOnly = true)
    public KardexResumenDTO resumen() {
        List<KardexMovimientoDTO> movimientos = movimientos();

        List<KardexMovimientoDTO> ultimos = movimientos.stream()
                .sorted(Comparator.comparing(KardexMovimientoDTO::fecha).reversed())
                .limit(5)
                .toList();
        List<KardexMovimientoDTO> ultimasEntradas = movimientos.stream()
                .filter(m -> "Entrada".equals(m.tipo()))
                .sorted(Comparator.comparing(KardexMovimientoDTO::fecha).reversed())
                .limit(3)
                .toList();

        long totalEntradas = movimientos.stream().filter(m -> "Entrada".equals(m.tipo())).count();
        long totalSalidas = movimientos.stream().filter(m -> "Salida".equals(m.tipo())).count();

        List<Producto> prods = productos.findAll();
        int stockTotal = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .mapToInt(p -> p.getStock() == null ? 0 : p.getStock())
                .sum();
        long stockCriticoBajo = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()) && p.getStock() != null && p.getStock() < 10)
                .count();

        return new KardexResumenDTO(
                movimientos.size(),
                totalEntradas,
                totalSalidas,
                prods.size(),
                stockTotal,
                (int) stockCriticoBajo,
                ultimos,
                ultimasEntradas);
    }
}
