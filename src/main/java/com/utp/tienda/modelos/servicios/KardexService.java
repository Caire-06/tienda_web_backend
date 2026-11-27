package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.DetalleEntrada;
import com.utp.tienda.modelos.entidades.DetallePedido;
import com.utp.tienda.modelos.entidades.Entrada;
import com.utp.tienda.modelos.entidades.InventarioInicial;
import com.utp.tienda.modelos.entidades.Pedido;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.repositorios.EntradaRepository;
import com.utp.tienda.modelos.repositorios.InventarioInicialRepository;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.vistas.dto.InventarioInicialDTO;
import com.utp.tienda.vistas.dto.KardexMovimientoDTO;
import com.utp.tienda.vistas.dto.KardexResumenDTO;
import com.utp.tienda.vistas.dto.ProductoDTO;
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

    private static final String INVENTARIO_INICIAL = "Inventario inicial";
    private static final String ENTRADA = "Entrada";
    private static final String SALIDA = "Salida";

    private final PedidoRepository pedidos;
    private final EntradaRepository entradas;
    private final ProductoRepository productos;
    private final ProductoService productoService;
    private final InventarioInicialRepository inventariosIniciales;

    public KardexService(
            PedidoRepository pedidos,
            EntradaRepository entradas,
            ProductoRepository productos,
            ProductoService productoService,
            InventarioInicialRepository inventariosIniciales
    ) {
        this.pedidos = pedidos;
        this.entradas = entradas;
        this.productos = productos;
        this.productoService = productoService;
        this.inventariosIniciales = inventariosIniciales;
    }

    /**
     * Construye el Kardex valorizado con promedio ponderado móvil.
     *
     * Fuentes (3FN, sin duplicar información):
     *  - inventarios_iniciales: stock físico inicial + costo unitario inicial;
     *  - detalle_entradas: compras (costo de compra);
     *  - detalle_pedidos: ventas (cantidad), valorizadas SIEMPRE con el costo promedio vigente,
     *    nunca con el precio de venta al cliente.
     */
    @Transactional(readOnly = true)
    public List<KardexMovimientoDTO> movimientos() {

        List<KardexMovimientoDTO> movimientos = new ArrayList<>();

        // 1. INVENTARIO INICIAL (uno por producto, con su costo unitario real).
        for (InventarioInicial inicial : inventariosIniciales.findByActivoTrue()) {

            Integer productoId = inicial.getProductoId();
            if (productoId == null) continue;

            BigDecimal costo = inicial.getCostoUnitario() != null
                    ? inicial.getCostoUnitario()
                    : BigDecimal.ZERO;
            int cantidad = inicial.getCantidad() != null ? inicial.getCantidad() : 0;

            movimientos.add(new KardexMovimientoDTO(
                    "II" + inicial.getIdInventarioInicial(),
                    inicial.getFecha() != null ? inicial.getFecha().toString() : "",
                    INVENTARIO_INICIAL,
                    productoId,
                    inicial.getProducto() != null ? inicial.getProducto().getNombre() : "",
                    cantidad,
                    costo,
                    costo.multiply(BigDecimal.valueOf(cantidad)),
                    null, null, null, null, null,
                    "", "",
                    INVENTARIO_INICIAL,
                    "Stock físico registrado al crear el producto",
                    inicial.getUsuarioRegistro() != null ? inicial.getUsuarioRegistro() : "SISTEMA",
                    false,
                    null
            ));
        }

        // 2. ENTRADAS DE MERCADERÍA (compras).
        for (Entrada entrada : entradas.findAll()) {

            for (DetalleEntrada detalle : entrada.getDetalles()) {

                BigDecimal costo = detalle.getCostoUnitario() != null
                        ? detalle.getCostoUnitario()
                        : BigDecimal.ZERO;

                int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;

                movimientos.add(new KardexMovimientoDTO(
                        "E" + entrada.getIdEntrada() + "-" + detalle.getIdDetalleEntrada(),
                        entrada.getFecha() != null ? entrada.getFecha().toString() : "",
                        ENTRADA,
                        detalle.getProductoId(),
                        detalle.getNombre(),
                        cantidad,
                        costo,
                        costo.multiply(BigDecimal.valueOf(cantidad)),
                        null, null, null, null, null,
                        detalle.getGuia(),
                        detalle.getFactura(),
                        detalle.getProveedor() != null
                                ? detalle.getProveedor().getNombre()
                                : "Compra",
                        "Ingreso de mercadería",
                        entrada.getUsuario() != null
                                ? entrada.getUsuario().getNombreUsuario()
                                : "",
                        false,
                        null
                ));
            }
        }

        // 3. SALIDAS POR VENTAS. El precio de venta NO se usa para valorizar la salida:
        //    el costo se calcula con el promedio ponderado vigente en el paso 5.
        for (Pedido pedido : pedidos.findAll()) {
            if ("Cancelado".equalsIgnoreCase(pedido.getEstado())) continue;

            for (DetallePedido detalle : pedido.getDetalles()) {

                int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;

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
                        SALIDA,
                        detalle.getProductoId(),
                        detalle.getNombre(),
                        cantidad,
                        null,
                        null,
                        null, null, null, null, null,
                        pedido.getDespachoLote() != null
                                ? pedido.getDespachoLote().getNumeroGuiaRemision()
                                : "",
                        factura,
                        "Venta " + pedido.getNumero(),
                        "Venta " + pedido.getNumero(),
                        pedido.getUsuario() != null
                                ? pedido.getUsuario().getNombreUsuario()
                                : "",
                        false,
                        null
                ));
            }
        }

        // 4. AGRUPAR POR PRODUCTO (el promedio es independiente por cada producto).
        Map<Integer, List<KardexMovimientoDTO>> porProducto = new LinkedHashMap<>();

        for (KardexMovimientoDTO movimiento : movimientos) {
            if (movimiento.productoId() == null) continue;
            porProducto.computeIfAbsent(movimiento.productoId(), k -> new ArrayList<>()).add(movimiento);
        }

        List<KardexMovimientoDTO> resultado = new ArrayList<>();

        // 5. PROMEDIO PONDERADO MÓVIL PRODUCTO POR PRODUCTO.
        for (Map.Entry<Integer, List<KardexMovimientoDTO>> entry : porProducto.entrySet()) {

            List<KardexMovimientoDTO> lista = entry.getValue();

            // Orden cronológico. Con la misma fecha: inicial -> compra -> venta,
            // para que las salidas siempre vean el costo vigente.
            lista.sort(
                    Comparator.comparing(this::obtenerFecha)
                            .thenComparing(m -> prioridad(m.tipo()))
                            .thenComparing(KardexMovimientoDTO::id)
            );

            int saldoCantidad = 0;
            BigDecimal saldoValor = BigDecimal.ZERO;

            for (KardexMovimientoDTO movimiento : lista) {

                int cantidad = movimiento.cantidad() != null ? movimiento.cantidad() : 0;
                int stockAnterior = saldoCantidad;
                BigDecimal valorAnterior = saldoValor;

                BigDecimal costoUnitario;
                BigDecimal valorMovimiento;
                boolean inconsistente = false;
                String observacion = null;

                if (SALIDA.equals(movimiento.tipo())) {

                    // Las salidas se valorizan con el promedio vigente, nunca con precio de venta.
                    if (saldoCantidad > 0 && saldoValor.signum() >= 0) {
                        costoUnitario = saldoValor.divide(
                                BigDecimal.valueOf(saldoCantidad), 8, RoundingMode.HALF_UP);
                        valorMovimiento = cantidad == saldoCantidad ? saldoValor : saldoValor.multiply(BigDecimal.valueOf(cantidad)).divide(BigDecimal.valueOf(saldoCantidad), 2, RoundingMode.HALF_UP);
                    } else {
                        // No hay existencias valorizadas: NO se inventa un costo.
                        costoUnitario = BigDecimal.ZERO;
                        valorMovimiento = BigDecimal.ZERO;
                        inconsistente = true;
                        observacion = "Salida sin existencias valorizadas; costo no disponible";
                    }

                    saldoValor = saldoValor.subtract(valorMovimiento);
                    saldoCantidad -= cantidad;

                    if (saldoCantidad < 0 || saldoValor.signum() < 0) {
                        inconsistente = true;
                        observacion = observacion == null
                                ? "Saldo negativo histórico: inconsistencia en el inventario"
                                : observacion + "; saldo negativo histórico";
                    }

                } else {
                    // Inventario inicial y compras: entran a costo real.
                    costoUnitario = movimiento.valorUnitario() != null
                            ? movimiento.valorUnitario()
                            : BigDecimal.ZERO;
                    valorMovimiento = costoUnitario.multiply(BigDecimal.valueOf(cantidad));

                    saldoValor = saldoValor.add(valorMovimiento);
                    saldoCantidad += cantidad;

                    // Un saldo valor negativo heredado se conserva visible y se marca.
                    if (saldoValor.signum() < 0) {
                        inconsistente = true;
                        observacion = "Saldo valor negativo heredado de saldos históricos";
                    }
                }

                BigDecimal saldoPromedio = saldoCantidad > 0 && saldoValor.signum() >= 0
                        ? saldoValor.divide(BigDecimal.valueOf(saldoCantidad), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                resultado.add(new KardexMovimientoDTO(
                        movimiento.id(),
                        movimiento.fecha(),
                        movimiento.tipo(),
                        movimiento.productoId(),
                        movimiento.producto(),
                        cantidad,
                        costoUnitario,
                        valorMovimiento,
                        stockAnterior,
                        saldoCantidad,
                        valorAnterior,
                        saldoValor,
                        saldoPromedio,
                        movimiento.guia(),
                        movimiento.factura(),
                        movimiento.detalle(),
                        movimiento.motivo(),
                        movimiento.usuario(),
                        inconsistente,
                        observacion
                ));
            }
        }

        // 6. MOSTRAR LOS MOVIMIENTOS MÁS RECIENTES PRIMERO.
        resultado.sort(
                Comparator.comparing(this::obtenerFecha)
                        .thenComparing(m -> prioridad(m.tipo()))
                        .thenComparing(KardexMovimientoDTO::id)
                        .reversed()
        );

        return resultado;
    }

    private int prioridad(String tipo) {
        if (INVENTARIO_INICIAL.equals(tipo)) return 0;
        if (ENTRADA.equals(tipo)) return 1;
        return 2;
    }

    /** Fecha del movimiento para ordenar cronológicamente. */
    private LocalDateTime obtenerFecha(KardexMovimientoDTO movimiento) {
        if (movimiento.fecha() == null || movimiento.fecha().isBlank()) {
            return LocalDateTime.MIN;
        }
        try {
            return LocalDateTime.parse(movimiento.fecha());
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }

    /**
     * Consulta movimientos con filtros. Los filtros se aplican DESPUÉS de calcular
     * todos los saldos, para no alterar el promedio ponderado histórico.
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
                .filter(m -> productoId == null || productoId.equals(m.productoId()))
                .filter(m -> desde == null || !obtenerFecha(m).toLocalDate().isBefore(desde))
                .filter(m -> hasta == null || !obtenerFecha(m).toLocalDate().isAfter(hasta))
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
                    List.of(), total, 1, pageable.getPageNumber(), pageable.getPageSize());
        }

        int fin = Math.min(inicio + pageable.getPageSize(), total);
        int totalPaginas = (int) Math.ceil((double) total / pageable.getPageSize());

        return new com.utp.tienda.vistas.dto.PaginaDTO<>(
                todos.subList(inicio, fin), total, Math.max(1, totalPaginas),
                pageable.getPageNumber(), pageable.getPageSize());
    }

    /** Resumen general del Kardex. */
    @Transactional(readOnly = true)
    public KardexResumenDTO resumen() {
        List<KardexMovimientoDTO> movimientos = movimientos();

        List<KardexMovimientoDTO> ultimos = movimientos.stream().limit(5).toList();
        List<KardexMovimientoDTO> ultimasEntradas = movimientos.stream()
                .filter(m -> ENTRADA.equals(m.tipo()) || INVENTARIO_INICIAL.equals(m.tipo()))
                .limit(3)
                .toList();

        long totalEntradas = movimientos.stream()
                .filter(m -> ENTRADA.equals(m.tipo()) || INVENTARIO_INICIAL.equals(m.tipo()))
                .count();
        long totalSalidas = movimientos.stream()
                .filter(m -> SALIDA.equals(m.tipo()))
                .count();

        List<Producto> prods = productos.findAll();
        int stockTotal = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .mapToInt(p -> p.getStock() == null ? 0 : p.getStock())
                .sum();
        long stockCriticoBajo = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo())
                        && p.getStock() != null && p.getStock() < 10)
                .count();

        return new KardexResumenDTO(
                movimientos.size(), totalEntradas, totalSalidas,
                prods.size(), stockTotal, (int) stockCriticoBajo,
                ultimos, ultimasEntradas
        );
    }

    @Transactional
    public ProductoDTO regularizarInventarioInicial(Integer usuarioId,
                                                    com.utp.tienda.vistas.dto.RegularizarInventarioDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Datos de regularización obligatorios");
        }
        return productoService.regularizarInventarioInicial(
                usuarioId, dto.productoId(), dto.costoUnitario());
    }

    @Transactional(readOnly = true)
    public List<InventarioInicialDTO> inventariosIniciales(Integer usuarioId) {
        productoService.verificarRolPermitido(usuarioId);
        return productoService.inventariosIniciales();
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> pendientesRegularizacion(Integer usuarioId) {
        productoService.verificarRolPermitido(usuarioId);
        return productoService.pendientesRegularizacion();
    }
}
