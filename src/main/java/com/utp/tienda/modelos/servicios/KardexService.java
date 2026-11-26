
package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.DetalleEntrada;
import com.utp.tienda.modelos.entidades.DetallePedido;
import com.utp.tienda.modelos.entidades.Entrada;
import com.utp.tienda.modelos.entidades.Pedido;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.repositorios.EntradaRepository;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.vistas.dto.KardexMovimientoDTO;
import com.utp.tienda.vistas.dto.KardexResumenDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public KardexService(
            PedidoRepository pedidos,
            EntradaRepository entradas,
            ProductoRepository productos,
            ProductoService productoService
    ) {
        this.pedidos = pedidos;
        this.entradas = entradas;
        this.productos = productos;
        this.productoService = productoService;
    }

    /**
     * Obtiene todos los movimientos y calcula el Kardex valorizado
     * utilizando el método de promedio ponderado móvil.
     */
    @Transactional(readOnly = true)
    public List<KardexMovimientoDTO> movimientos() {

        List<KardexMovimientoDTO> movimientos = new ArrayList<>();

        // ENTRADAS DE MERCADERÍA
        for (Entrada entrada : entradas.findAll()) {

            for (DetalleEntrada detalle : entrada.getDetalles()) {

                BigDecimal costo = detalle.getCostoUnitario() != null
                        ? detalle.getCostoUnitario()
                        : BigDecimal.ZERO;

                int cantidad = detalle.getCantidad() != null
                        ? detalle.getCantidad()
                        : 0;

                movimientos.add(new KardexMovimientoDTO(
                        "E" + entrada.getIdEntrada() + "-" + detalle.getIdDetalleEntrada(),
                        entrada.getFecha() != null ? entrada.getFecha().toString() : "",
                        "Entrada",
                        detalle.getProductoId(),
                        detalle.getNombre(),
                        cantidad,
                        costo,
                        costo.multiply(BigDecimal.valueOf(cantidad)),
                        null,
                        null,
                        null,
                        null,
                        null,
                        detalle.getGuia(),
                        detalle.getFactura(),
                        detalle.getProveedor() != null
                                ? detalle.getProveedor().getNombre()
                                : "Compra",
                        "Ingreso de mercadería",
                        entrada.getUsuario() != null
                                ? entrada.getUsuario().getNombreUsuario()
                                : ""
                ));
            }
        }

        // SALIDAS POR VENTAS
        for (Pedido pedido : pedidos.findAll()) {

            for (DetallePedido detalle : pedido.getDetalles()) {

                BigDecimal precio = detalle.getPrecioUnitario() != null
                        ? detalle.getPrecioUnitario()
                        : BigDecimal.ZERO;

                int cantidad = detalle.getCantidad() != null
                        ? detalle.getCantidad()
                        : 0;

                String factura = "";

                if (pedido.getComprobante() != null
                        && pedido.getComprobante().getTipo() != null) {

                    factura = ("FACTURA".equalsIgnoreCase(
                            pedido.getComprobante().getTipo()
                    ) ? "F001-" : "B001-") + pedido.getNumero();
                }

                movimientos.add(new KardexMovimientoDTO(
                        "P" + pedido.getIdPedido() + "-" + detalle.getIdDetallePedido(),
                        pedido.getFecha() != null ? pedido.getFecha().toString() : "",
                        "Salida",
                        detalle.getProductoId(),
                        detalle.getNombre(),
                        cantidad,
                        precio,
                        precio.multiply(BigDecimal.valueOf(cantidad)),
                        null,
                        null,
                        null,
                        null,
                        null,
                        pedido.getDespachoLote() != null
                                ? pedido.getDespachoLote().getNumeroGuiaRemision()
                                : "",
                        factura,
                        "Venta " + pedido.getNumero(),
                        "Venta " + pedido.getNumero(),
                        pedido.getUsuario() != null
                                ? pedido.getUsuario().getNombreUsuario()
                                : ""
                ));
            }
        }

        // AGRUPAR MOVIMIENTOS POR PRODUCTO
        Map<Integer, List<KardexMovimientoDTO>> porProducto = new LinkedHashMap<>();

        for (KardexMovimientoDTO movimiento : movimientos) {

            if (movimiento.productoId() == null) {
                continue;
            }

            porProducto.computeIfAbsent(
                    movimiento.productoId(),
                    k -> new ArrayList<>()
            ).add(movimiento);
        }

        List<KardexMovimientoDTO> resultado = new ArrayList<>();

        // CALCULAR PROMEDIO PONDERADO MÓVIL
        for (Map.Entry<Integer, List<KardexMovimientoDTO>> entry : porProducto.entrySet()) {

            List<KardexMovimientoDTO> lista = entry.getValue();

            // Orden cronológico. Si coinciden las fechas, se procesan
            // primero las entradas y después las salidas.
            lista.sort(
                    Comparator.comparing(this::obtenerFecha)
                            .thenComparing(m -> "Entrada".equals(m.tipo()) ? 0 : 1)
                            .thenComparing(KardexMovimientoDTO::id)
            );

            int saldoCantidad = 0;
            BigDecimal saldoValor = BigDecimal.ZERO;

            for (KardexMovimientoDTO movimiento : lista) {

                int cantidad = movimiento.cantidad() != null
                        ? movimiento.cantidad()
                        : 0;

                int stockAnterior = saldoCantidad;
                BigDecimal valorAnterior = saldoValor;

                BigDecimal costoUnitario;

                if ("Entrada".equals(movimiento.tipo())) {

                    // Una entrada incorpora mercadería al costo de compra.
                    costoUnitario = movimiento.valorUnitario() != null
                            ? movimiento.valorUnitario()
                            : BigDecimal.ZERO;

                    BigDecimal valorEntrada = costoUnitario.multiply(
                            BigDecimal.valueOf(cantidad)
                    );

                    saldoValor = saldoValor.add(valorEntrada);
                    saldoCantidad += cantidad;

                } else {

                    // Una salida utiliza el costo promedio vigente.
                    costoUnitario = saldoCantidad > 0
                            ? saldoValor.divide(
                                    BigDecimal.valueOf(saldoCantidad),
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            : BigDecimal.ZERO;

                    BigDecimal valorSalida = costoUnitario.multiply(
                            BigDecimal.valueOf(cantidad)
                    );

                    saldoValor = saldoValor.subtract(valorSalida);
                    saldoCantidad -= cantidad;
                }

                BigDecimal saldoPromedio = saldoCantidad > 0
                        ? saldoValor.divide(
                                BigDecimal.valueOf(saldoCantidad),
                                2,
                                RoundingMode.HALF_UP
                        )
                        : BigDecimal.ZERO;

                KardexMovimientoDTO ajustada = new KardexMovimientoDTO(
                        movimiento.id(),
                        movimiento.fecha(),
                        movimiento.tipo(),
                        movimiento.productoId(),
                        movimiento.producto(),
                        cantidad,
                        costoUnitario,
                        costoUnitario.multiply(BigDecimal.valueOf(cantidad)),
                        stockAnterior,
                        saldoCantidad,
                        valorAnterior,
                        saldoValor,
                        saldoPromedio,
                        movimiento.guia(),
                        movimiento.factura(),
                        movimiento.detalle(),
                        movimiento.motivo(),
                        movimiento.usuario()
                );

                resultado.add(ajustada);
            }
        }

        // MOSTRAR LOS MOVIMIENTOS MÁS RECIENTES PRIMERO
        resultado.sort(
                Comparator.comparing(this::obtenerFecha)
                        .reversed()
                        .thenComparing(KardexMovimientoDTO::id)
        );

        return resultado;
    }

    /**
     * Devuelve la fecha del movimiento para ordenar cronológicamente.
     */
    private LocalDateTime obtenerFecha(KardexMovimientoDTO movimiento) {

        if (movimiento.fecha() == null || movimiento.fecha().isBlank()) {
            return LocalDateTime.MIN;
        }

        return LocalDateTime.parse(movimiento.fecha());
    }

    /**
     * Consulta movimientos con filtros.
     * Los filtros se aplican después de calcular todos los saldos,
     * para no alterar el promedio ponderado histórico.
     */
    @Transactional(readOnly = true)
    public List<KardexMovimientoDTO> movimientos(
            Integer usuarioId,
            Integer productoId,
            LocalDate desde,
            LocalDate hasta
    ) {

        productoService.verificarRolPermitido(usuarioId);

        return movimientos().stream()
                .filter(m -> productoId == null
                        || productoId.equals(m.productoId()))
                .filter(m -> {
                    if (desde == null) return true;

                    return !obtenerFecha(m).toLocalDate().isBefore(desde);
                })
                .filter(m -> {
                    if (hasta == null) return true;

                    return !obtenerFecha(m).toLocalDate().isAfter(hasta);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KardexMovimientoDTO> movimientos(Integer usuarioId) {
        productoService.verificarRolPermitido(usuarioId);
        return movimientos();
    }

    @Transactional(readOnly = true)
    public com.utp.tienda.vistas.dto.PaginaDTO<KardexMovimientoDTO> movimientosPaginado(
            Integer usuarioId,
            Pageable pageable
    ) {

        productoService.verificarRolPermitido(usuarioId);

        List<KardexMovimientoDTO> todos = movimientos();

        int total = todos.size();
        int inicio = (int) pageable.getOffset();

        if (inicio >= total) {
            return new com.utp.tienda.vistas.dto.PaginaDTO<>(
                    List.of(),
                    total,
                    (int) Math.ceil((double) total / pageable.getPageSize()),
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
        }

        int fin = Math.min(inicio + pageable.getPageSize(), total);

        List<KardexMovimientoDTO> sublista = todos.subList(inicio, fin);

        int totalPaginas = (int) Math.ceil(
                (double) total / pageable.getPageSize()
        );

        return new com.utp.tienda.vistas.dto.PaginaDTO<>(
                sublista,
                total,
                Math.max(1, totalPaginas),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    /**
     * Resumen general del Kardex.
     */
    @Transactional(readOnly = true)
    public KardexResumenDTO resumen() {

        List<KardexMovimientoDTO> movimientos = movimientos();

        List<KardexMovimientoDTO> ultimos = movimientos.stream()
                .limit(5)
                .toList();

        List<KardexMovimientoDTO> ultimasEntradas = movimientos.stream()
                .filter(m -> "Entrada".equals(m.tipo()))
                .limit(3)
                .toList();

        long totalEntradas = movimientos.stream()
                .filter(m -> "Entrada".equals(m.tipo()))
                .count();

        long totalSalidas = movimientos.stream()
                .filter(m -> "Salida".equals(m.tipo()))
                .count();

        List<Producto> prods = productos.findAll();

        int stockTotal = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .mapToInt(p -> p.getStock() == null ? 0 : p.getStock())
                .sum();

        long stockCriticoBajo = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo())
                        && p.getStock() != null
                        && p.getStock() < 10)
                .count();

        return new KardexResumenDTO(
                movimientos.size(),
                totalEntradas,
                totalSalidas,
                prods.size(),
                stockTotal,
                (int) stockCriticoBajo,
                ultimos,
                ultimasEntradas
        );
    }
}
