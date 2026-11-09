package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PedidoService {
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("d/M/yyyy", new Locale("es", "PE"));

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

        Pedido pedido = new Pedido();
        pedido.setNumero(generarNumero());
        pedido.setFecha(LocalDateTime.now());
        pedido.setUsuario(usuario);
        pedido.setEstado("Pendiente");
        pedido.setTipoEntrega(valor(dto.tipoEntrega(), "envio"));
        pedido.setMetodoPago(dto.metodoPago());
        pedido.setCorreoContacto(dto.correoContacto());
        pedido.setCelularContacto(dto.celularContacto());
        pedido.setDireccionEnvio(dto.direccionEnvio());
        pedido.setReferenciaEnvio(dto.referenciaEnvio());
        pedido.setDistritoEnvio(dto.distritoEnvio());
        pedido.setTotal(dto.total() != null ? dto.total() : BigDecimal.ZERO);

        List<DetallePedido> detalles = new ArrayList<>();
        if (dto.items() != null) {
            for (ItemPedidoDTO item : dto.items()) {
                DetallePedido det = new DetallePedido();
                det.setPedido(pedido);
                det.setProductoId(item.id());
                det.setNombre(item.nombre());
                det.setPrecioUnitario(item.precio() != null ? item.precio() : BigDecimal.ZERO);
                det.setCantidad(item.cantidad() != null ? item.cantidad() : 1);
                det.setImagen(item.imagen());
                detalles.add(det);
            }
        }
        pedido.setDetalles(detalles);

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

    private PedidoDTO mapear(Pedido p) {
        List<ItemPedidoDTO> items = p.getDetalles().stream()
                .map(d -> new ItemPedidoDTO(d.getProductoId(), d.getNombre(), d.getPrecioUnitario(), d.getCantidad(), d.getImagen()))
                .toList();
        ComprobanteDTO comp = null;
        if (p.getComprobante() != null) {
            ComprobantePedido c = p.getComprobante();
            comp = new ComprobanteDTO(c.getTipo(), c.getDocumento(), c.getNombreRazonSocial(), c.getDireccionFiscal(), c.getSubtotal(), c.getIgv(), c.getTotal());
        }
        String fecha = p.getFecha() != null ? p.getFecha().format(FECHA_FMT) : "";
        return new PedidoDTO(p.getNumero(), fecha, p.getEstado(), p.getTipoEntrega(), p.getMetodoPago(), p.getTotal(), items, comp);
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
