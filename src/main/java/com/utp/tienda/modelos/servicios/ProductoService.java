package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.CrearProductoDTO;
import com.utp.tienda.vistas.dto.ProductoDTO;
import com.utp.tienda.vistas.dto.ActualizarProductoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {
    private static final List<String> ROLES_PERMITIDOS = List.of("ADMINISTRADOR", "ALMACEN");

    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    public ProductoService(ProductoRepository productos, UsuarioRepository usuarios) {
        this.productos = productos;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listar() {
        return productos.findAllByOrderByNombreAsc().stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDTO detalle(Integer id) {
        Producto p = productos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        return mapear(p);
    }

    @Transactional
    public ProductoDTO crear(Integer usuarioId, CrearProductoDTO dto) {
        verificarRolPermitido(usuarioId);
        Producto p = new Producto();
        p.setNombre(dto.nombre());
        p.setCategoria(dto.categoria());
        p.setPrecio(dto.precio());
        p.setStock(dto.stock());
        p.setVendidos(dto.vendidos() != null ? dto.vendidos() : 0);
        p.setImagen(dto.imagen());
        p.setDescripcion(dto.descripcion());
        p.setActivo(dto.activo() != null ? dto.activo() : true);
        return mapear(productos.save(p));
    }

    @Transactional
    public ProductoDTO actualizar(Integer usuarioId, Integer id, ActualizarProductoDTO dto) {
        verificarRolPermitido(usuarioId);
        Producto p = productos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (dto.nombre() != null) p.setNombre(dto.nombre());
        if (dto.categoria() != null) p.setCategoria(dto.categoria());
        if (dto.precio() != null) p.setPrecio(dto.precio());
        if (dto.stock() != null) p.setStock(dto.stock());
        if (dto.imagen() != null) p.setImagen(dto.imagen());
        if (dto.descripcion() != null) p.setDescripcion(dto.descripcion());
        if (dto.vendidos() != null) p.setVendidos(dto.vendidos());
        if (dto.activo() != null) p.setActivo(dto.activo());
        return mapear(productos.save(p));
    }

    @Transactional
    public void eliminar(Integer id) {
        Producto p = productos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        productos.delete(p);
    }

    private void verificarRolPermitido(Integer usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String rol = u.getRol() != null ? u.getRol().getNombreRol() : null;
        if (!ROLES_PERMITIDOS.contains(rol)) {
            throw new IllegalArgumentException("No tiene permisos para gestionar productos");
        }
    }

    private ProductoDTO mapear(Producto p) {
        return new ProductoDTO(p.getIdProducto(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getStock(), p.getVendidos(), p.getImagen(), p.getDescripcion(), p.getActivo());
    }
}
