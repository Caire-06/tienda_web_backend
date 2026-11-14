package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.patrones.strategy.EstrategiaCostoEnvio;
import com.utp.tienda.vistas.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {
    private static final DateTimeFormatter FECHA_ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final PerfilService perfilService;
    private final ProductoRepository productos;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, PerfilService perfilService, ProductoRepository productos) {
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.perfilService = perfilService;
        this.productos = productos;
    }

    @Transactional
    public PedidoDTO crear(Integer usuarioId, CrearPedidoDTO dto) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String tipoEntrega = valor(dto.tipoEntrega(), "delivery_propio");
        String metodoPago = valor(dto.metodoPago(), "tarjeta").trim().toLowerCase();

        if (!Set.of("tarjeta", "yape", "transferencia").contains(metodoPago)) {
            throw new IllegalArgumentException("Método de pago no válido: " + dto.metodoPago());
        }
        if (!Set.of("delivery_propio", "agencia_destino", "recojo").contains(tipoEntrega)) {
            throw new IllegalArgumentException("Modalidad de entrega no válida: " + dto.tipoEntrega());
        }
        if ("delivery_propio".equalsIgnoreCase(tipoEntrega)) {
            if (dto.correoContacto() == null || dto.correoContacto().isBlank()) {
                throw new IllegalArgumentException("Para la modalidad de delivery propio debes ingresar el correo electrónico de contacto.");
            }
            if (dto.celularContacto() == null || dto.celularContacto().isBlank()) {
                throw new IllegalArgumentException("Para la modalidad de delivery propio debes ingresar el teléfono celular de contacto.");
            }
            if (dto.direccionEnvio() == null || dto.direccionEnvio().isBlank()) {
                throw new IllegalArgumentException("Para la modalidad de delivery propio debes ingresar la dirección de envío.");
            }
        }
        String opPago = dto.codigoOperacionPago() != null ? dto.codigoOperacionPago().trim() : "";
        String urlComprobantePago = dto.comprobantePagoUrl() != null ? dto.comprobantePagoUrl().trim() : "";

        if ("yape".equals(metodoPago)) {
            if (!opPago.matches("^\\d{6,8}(\\s*·\\s*Seg:\\s*\\d{3})?$")) {
                throw new IllegalArgumentException(
                        "Ingresa un número de operación válido de Yape/Plin (6 a 8 dígitos numéricos).");
            }
        }
        if ("transferencia".equals(metodoPago) && opPago.isBlank() && urlComprobantePago.isBlank()) {
            throw new IllegalArgumentException("Para pagar por transferencia ingresa el número de operación bancaria.");
        }

        Pedido pedido = new Pedido();
        pedido.setNumero(generarNumero());
        pedido.setFecha(LocalDateTime.now());
        pedido.setUsuario(usuario);
        if ("transferencia".equals(metodoPago)) {
            pedido.setEstado("Pendiente de pago");
            pedido.setEstadoPago("Por verificar");
        } else {
            pedido.setEstado("Pendiente");
            pedido.setEstadoPago("Pagado");
        }
        pedido.setTipoEntrega(tipoEntrega);
        pedido.setMetodoPago(metodoPago);
        pedido.setCodigoOperacionPago(opPago.isBlank() ? null : opPago);
        pedido.setComprobantePagoUrl(urlComprobantePago.isBlank() ? null : urlComprobantePago);
        pedido.setCorreoContacto(dto.correoContacto());
        pedido.setCelularContacto(dto.celularContacto());
        pedido.setDireccionEnvio(dto.direccionEnvio());
        pedido.setReferenciaEnvio(dto.referenciaEnvio());
        pedido.setDistritoEnvio(dto.distritoEnvio());

        BigDecimal subtotalItems = BigDecimal.ZERO;
        List<DetallePedido> detalles = new ArrayList<>();
        if (dto.items() != null) {
            for (ItemPedidoDTO item : dto.items()) {
                // Lock pesimoso sobre la fila del producto + validación de stock dentro de la transacción
                Producto prod = productos.findByIdConBloqueo(item.id())
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + item.id()));
                if (!Boolean.TRUE.equals(prod.getActivo())) {
                    throw new IllegalArgumentException("El producto no está activo: " + item.nombre());
                }
                int cant = item.cantidad() != null ? item.cantidad() : 1;
                if (cant < 1) {
                    throw new IllegalArgumentException("La cantidad del producto debe ser mayor o igual a 1");
                }
                int stockActual = prod.getStock() != null ? prod.getStock() : 0;
                if (stockActual < cant) {
                    throw new IllegalArgumentException("No hay stock suficiente para el producto: " + item.nombre()
                            + " (disponible: " + stockActual + ")");
                }
                BigDecimal precio = prod.getPrecio() != null ? prod.getPrecio() : BigDecimal.ZERO;
                DetallePedido det = new DetallePedido();
                det.setPedido(pedido);
                det.setProductoId(item.id());
                det.setNombre(item.nombre());
                det.setPrecioUnitario(precio);
                det.setCantidad(cant);
                det.setImagen(item.imagen() != null ? item.imagen() : prod.getImagen());
                detalles.add(det);
                subtotalItems = subtotalItems.add(precio.multiply(BigDecimal.valueOf(cant)));

                // Descuento de stock dentro de la misma transacción (rollback automático si falla)
                prod.setStock(stockActual - cant);
                prod.setVendidos((prod.getVendidos() != null ? prod.getVendidos() : 0) + cant);
            }
        }
        pedido.setDetalles(detalles);

        // Cálculo oficial del costo de envío (reglas de negocio) y total final en el backend
        BigDecimal costoEnvioCalculado = EstrategiaCostoEnvio.obtenerPorModalidad(tipoEntrega).calcular(subtotalItems);
        pedido.setCostoEnvio(costoEnvioCalculado);
        pedido.setTotal(subtotalItems.add(costoEnvioCalculado));

        if (dto.comprobante() != null) {
            ComprobanteDTO c = dto.comprobante();
            ComprobantePedido comp = new ComprobantePedido();
            comp.setPedido(pedido);
            comp.setTipo(c.tipo());
            comp.setDocumento(c.documento());
            comp.setNombreRazonSocial(c.nombreRazonSocial());
            comp.setDireccionFiscal(c.direccion());
            comp.setSubtotal(c.subtotal() != null ? c.subtotal() : BigDecimal.ZERO);
            comp.setIgv(c.igv() != null ? c.igv() : BigDecimal.ZERO);
            comp.setTotal(c.total() != null ? c.total() : BigDecimal.ZERO);
            pedido.setComprobante(comp);
        }

        Pedido guardado = pedidos.save(pedido);
        perfilService.sincronizarDesdeCompra(usuario, dto);
        return mapear(guardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> misCompras(Integer usuarioId) {
        return pedidos.findByUsuario_IdUsuarioOrderByFechaDesc(usuarioId).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidos.findAllByOrderByFechaDesc().stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional
    public PedidoDTO actualizarEstado(String numero, String nuevoEstado) {
        return actualizarEstado(null, numero, nuevoEstado);
    }

    @Transactional
    public PedidoDTO actualizarEstado(Integer usuarioId, String numero, String nuevoEstado) {
        if (usuarioId != null) {
            Usuario u = usuarios.findById(usuarioId).orElse(null);
            String rol = u != null && u.getRol() != null ? u.getRol().getNombreRol() : "";
            if ("CLIENTE".equalsIgnoreCase(rol)) {
                throw new SecurityException("Solo personal autorizado de Almacén puede cambiar el estado del pedido.");
            }
        }
        Pedido p = pedidos.findByNumero(numero)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado: " + numero));

        boolean pagoSinConfirmar = "Por verificar".equalsIgnoreCase(p.getEstadoPago())
                || "Pendiente de pago".equalsIgnoreCase(p.getEstado());
        if (pagoSinConfirmar && !"Cancelado".equalsIgnoreCase(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "El pago por transferencia de este pedido aún está pendiente de confirmación por Administración.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if ("Procesando".equalsIgnoreCase(nuevoEstado)) {
            if (p.getFechaProcesando() == null) p.setFechaProcesando(ahora);
        } else if ("Enviado".equalsIgnoreCase(nuevoEstado) || "Listo para recojo".equalsIgnoreCase(nuevoEstado)) {
            if (p.getFechaProcesando() == null) p.setFechaProcesando(ahora);
            if (p.getFechaEnvio() == null) p.setFechaEnvio(ahora);
        } else if ("Entregado".equalsIgnoreCase(nuevoEstado)) {
            if (p.getFechaProcesando() == null) p.setFechaProcesando(ahora);
            if (p.getFechaEnvio() == null) p.setFechaEnvio(ahora);
            if (p.getFechaEntrega() == null) p.setFechaEntrega(ahora);
        }

        p.setEstado(nuevoEstado);
        return mapear(pedidos.save(p));
    }

    @Transactional
    public PedidoDTO confirmarPagoTransferencia(Integer adminUsuarioId, String numero, boolean aprobado) {
        Usuario admin = usuarios.findById(adminUsuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String rol = admin.getRol() != null ? admin.getRol().getNombreRol() : "";
        if (!"ADMINISTRADOR".equalsIgnoreCase(rol)) {
            throw new SecurityException("Solo el Administrador puede confirmar o rechazar transferencias bancarias.");
        }

        Pedido p = pedidos.findByNumero(numero)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado: " + numero));

        if (!"Por verificar".equalsIgnoreCase(p.getEstadoPago()) && !"Pendiente de pago".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalArgumentException("Este pedido no está pendiente de verificación de pago.");
        }

        p.setFechaConfirmacionPago(LocalDateTime.now());
        p.setUsuarioConfirmoPago(admin.getNombreUsuario());
        if (aprobado) {
            p.setEstadoPago("Pagado");
            p.setEstado("Pendiente");
        } else {
            p.setEstadoPago("Rechazado");
            p.setEstado("Cancelado");
        }
        return mapear(pedidos.save(p));
    }

    public PedidoDTO mapear(Pedido p) {
        List<ItemPedidoDTO> items = p.getDetalles().stream()
                .map(d -> new ItemPedidoDTO(d.getProductoId(), d.getNombre(), d.getPrecioUnitario(), d.getCantidad(), d.getImagen()))
                .toList();
        ComprobanteDTO comp = null;
        if (p.getComprobante() != null) {
            ComprobantePedido c = p.getComprobante();
            comp = new ComprobanteDTO(c.getTipo(), c.getDocumento(), c.getNombreRazonSocial(), c.getDireccionFiscal(), c.getSubtotal(), c.getIgv(), c.getTotal());
        }
        String fecha = p.getFecha() != null ? p.getFecha().format(FECHA_ISO_FMT) : "";
        String fechaProc = p.getFechaProcesando() != null ? p.getFechaProcesando().format(FECHA_ISO_FMT) : null;
        String fechaEnv = p.getFechaEnvio() != null ? p.getFechaEnvio().format(FECHA_ISO_FMT) : null;
        String fechaEnt = p.getFechaEntrega() != null ? p.getFechaEntrega().format(FECHA_ISO_FMT) : null;

        String cliente = "Cliente";
        if (p.getComprobante() != null && p.getComprobante().getNombreRazonSocial() != null && !p.getComprobante().getNombreRazonSocial().isBlank()) {
            cliente = p.getComprobante().getNombreRazonSocial();
        } else if (p.getUsuario() != null) {
            cliente = p.getUsuario().getNombreUsuario();
        }
        String codigoLote = p.getDespachoLote() != null ? p.getDespachoLote().getCodigoLote() : null;
        String empresa = p.getDespachoLote() != null ? p.getDespachoLote().getEmpresaTransporte() : null;
        BigDecimal costoEnvio = p.getCostoEnvio() != null ? p.getCostoEnvio() : BigDecimal.ZERO;
        String estadoPago = valor(p.getEstadoPago(), "Pagado");
        String fechaConfPago = p.getFechaConfirmacionPago() != null ? p.getFechaConfirmacionPago().format(FECHA_ISO_FMT) : null;

        return new PedidoDTO(
                p.getNumero(), fecha, cliente, p.getEstado(), p.getTipoEntrega(),
                p.getMetodoPago(), costoEnvio, p.getTotal(), items, comp,
                codigoLote, empresa, fechaProc, fechaEnv, fechaEnt,
                estadoPago, p.getCodigoOperacionPago(), p.getComprobantePagoUrl(),
                fechaConfPago, p.getUsuarioConfirmoPago()
        );
    }

    private String generarNumero() {
        for (int i = 0; i < 5; i++) {
            String numero = "TS-" + String.valueOf(System.currentTimeMillis()).replaceAll("\\D", "");
            numero = "TS-" + numero.substring(Math.max(3, numero.length() - 6));
            if (!pedidos.existsByNumero(numero)) return numero;
        }
        return "TS-" + (System.currentTimeMillis() % 1000000);
    }

    private String valor(String v, String defecto) {
        return (v == null || v.isBlank()) ? defecto : v;
    }
}
