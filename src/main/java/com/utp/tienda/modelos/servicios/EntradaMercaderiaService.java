package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.DetalleEntrada;
import com.utp.tienda.modelos.entidades.Entrada;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.EntradaRepository;
import com.utp.tienda.modelos.repositorios.DetalleEntradaRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.modelos.servicios.ProductoService;
import com.utp.tienda.vistas.dto.CrearEntradaDTO;
import com.utp.tienda.vistas.dto.EntradaDTO;
import com.utp.tienda.vistas.dto.ItemEntradaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EntradaMercaderiaService {
    private final EntradaRepository entradas;
    private final DetalleEntradaRepository detalles;
    private final ProductoRepository productos;
    private final ProductoService productoService;
    private final UsuarioRepository usuarios;

    public EntradaMercaderiaService(EntradaRepository entradas,
                                   DetalleEntradaRepository detalles,
                                   ProductoRepository productos,
                                   ProductoService productoService,
                                   UsuarioRepository usuarios) {
        this.entradas = entradas;
        this.detalles = detalles;
        this.productos = productos;
        this.productoService = productoService;
        this.usuarios = usuarios;
    }

    /** Registra una entrada de mercadería (varias líneas de productos) y actualiza el stock. */
    @Transactional
    public EntradaDTO registrarEntrada(Integer usuarioId, CrearEntradaDTO dto) {
        productoService.verificarRolPermitido(usuarioId);

        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Entrada entrada = new Entrada();
        entrada.setFecha(LocalDateTime.now());
        entrada.setUsuario(usuario);

        List<DetalleEntrada> lineas = new ArrayList<>();
        if (dto.items() != null) {
            for (ItemEntradaDTO item : dto.items()) {
                Producto producto = productos.findById(item.idProducto())
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + item.idProducto()));
                int cantidad = item.cantidad() != null ? item.cantidad() : 0;
                if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
                BigDecimal costo = item.costoUnitario() != null ? item.costoUnitario() : BigDecimal.ZERO;

                // La entrada de mercadería suma stock al producto (Kardex de ingresos).
                int stockActual = producto.getStock() != null ? producto.getStock() : 0;
                producto.setStock(stockActual + cantidad);

                DetalleEntrada detalle = new DetalleEntrada();
                detalle.setEntrada(entrada);
                detalle.setProductoId(producto.getIdProducto());
                detalle.setNombre(producto.getNombre());
                detalle.setCostoUnitario(costo);
                detalle.setCantidad(cantidad);
                lineas.add(detalle);
            }
        }
        entrada.setDetalles(lineas);
        Entrada guardada = entradas.save(entrada);
        return mapear(guardada);
    }

    @Transactional(readOnly = true)
    public List<EntradaDTO> misEntradas(Integer usuarioId) {
        return entradas.findByUsuario_IdUsuarioOrderByFechaDesc(usuarioId).stream()
                .map(this::mapear)
                .toList();
    }

    private EntradaDTO mapear(Entrada e) {
        List<ItemEntradaDTO> items = e.getDetalles().stream()
                .map(d -> new ItemEntradaDTO(d.getProductoId(), d.getCantidad(), d.getCostoUnitario()))
                .toList();
        return new EntradaDTO(e.getIdEntrada(), e.getFecha() != null ? e.getFecha().toString() : "", items);
    }
}
