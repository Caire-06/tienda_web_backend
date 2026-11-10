package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.patrones.strategy.EstrategiaCostoEnvio;
import com.utp.tienda.vistas.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {
    private static final DateTimeFormatter FECHA_ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios) {
        this.pedidos = pedidos;
        this.usuarios = usuarios;
    }

    @Transactional
    public PedidoDTO crear(Integer usuarioId, CrearPedidoDTO dto) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String tipoEntrega = valor(dto.tipoEntrega(), "delivery_propio");

        Pedido pedido = new Pedido();
        pedido.setNumero(generarNumero());
        pedido.setFecha(LocalDateTime.now());
        pedido.setUsuario(usuario);
        pedido.setEstado("Pendiente");
        pedido.setTipoEntrega(tipoEntrega);
        pedido.setMetodoPago(dto.metodoPago());
        pedido.setCorreoContacto(dto.correoContacto());
        pedido.setCelularContacto(dto.celularContacto());
        pedido.setDireccionEnvio(dto.direccionEnvio());
        pedido.setReferenciaEnvio(dto.referenciaEnvio());
        pedido.setDistritoEnvio(dto.distritoEnvio());

        BigDecimal subtotalItems = BigDecimal.ZERO;
        List<DetallePedido> detalles = new ArrayList<>();
        if (dto.items() != null) {
            for (ItemPedidoDTO item : dto.items()) {
                DetallePedido det = new DetallePedido();
                det.setPedido(pedido);
                det.setProductoId(item.id());
                det.setNombre(item.nombre());
                BigDecimal precio = item.precio() != null ? item.precio() : BigDecimal.ZERO;
                int cant = item.cantidad() != null ? item.cantidad() : 1;
                det.setPrecioUnitario(precio);
                det.setCantidad(cant);
                det.setImagen(item.imagen());
                detalles.add(det);
                subtotalItems = subtotalItems.add(precio.multiply(BigDecimal.valueOf(cant)));
            }
        }
        pedido.setDetalles(detalles);

        // Aplicación del Patrón Strategy para flete / envío
        BigDecimal costoEnvioCalculado = dto.costoEnvio() != null
                ? dto.costoEnvio()
                : EstrategiaCostoEnvio.obtenerPorModalidad(tipoEntrega).calcular(subtotalItems);
        pedido.setCostoEnvio(costoEnvioCalculado);

        pedido.setTotal(dto.total() != null ? dto.total() : subtotalItems.add(costoEnvioCalculado));

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
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + numero));

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

        return new PedidoDTO(
                p.getNumero(), fecha, cliente, p.getEstado(), p.getTipoEntrega(),
                p.getMetodoPago(), costoEnvio, p.getTotal(), items, comp,
                codigoLote, empresa, fechaProc, fechaEnv, fechaEnt
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
