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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {
    private static final List<String> ROLES_PERMITIDOS = List.of("ADMINISTRADOR", "ALMACEN");

    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final CloudinaryService cloudinaryService;

    public ProductoService(ProductoRepository productos, UsuarioRepository usuarios, CloudinaryService cloudinaryService) {
        this.productos = productos;
        this.usuarios = usuarios;
        this.cloudinaryService = cloudinaryService;
    }

    /** Datos de los productos iniciales (información actual de la tienda) + imagen local a subir a Cloudinary. */
    private record ProductoSeed(String nombre, String categoria, BigDecimal precio, Integer stock, Integer vendidos, String imagen, String descripcion) {}

    private static final List<ProductoSeed> PRODUCTOS_INICIALES = List.of(
            new ProductoSeed("Mouse Logitech G502 HERO", "Periféricos", new BigDecimal("229"), 20, 38, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283457/techstore/productos/x0tf8pobbhkai8ja3rki.webp", "Mouse gamer alámbrico 25K DPI, 11 botones programables."),
            new ProductoSeed("Audífonos Sony WH-1000XM4", "Audio", new BigDecimal("1049"), 12, 45, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794282995/techstore/productos/wq2rgb2dqa3brrbeqxjj.webp", "Audífonos bluetooth con noise cancelling, 30h de batería."),
            new ProductoSeed("Teclado Redragon Fizz Pro K616 RGB", "Periféricos", new BigDecimal("215"), 0, 25, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283650/techstore/productos/otje91i9tsezcdkaso9o.webp", "Teclado mecánico inalámbrico, RGB, 87 teclas."),
            new ProductoSeed("Laptop Lenovo IdeaPad Slim 3 Ryzen 7 / 16GB / 512GB", "Computadoras", new BigDecimal("2599"), 8, 12, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283309/techstore/productos/pqnx12wxjenwsjti2t6k.webp", "15.6\" FHD, Ryzen 7 serie 5000, 16GB RAM, 512GB SSD."),
            new ProductoSeed("Samsung Galaxy A55 5G 8GB / 128GB", "Smartphones", new BigDecimal("1481"), 15, 30, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283605/techstore/productos/vdjvpqg0bp7c3155bkns.webp", "Pantalla 6.6\" Super AMOLED 120Hz, cámara 50MP, batería 5000mAh."),
            new ProductoSeed("Kit Cargador Xiaomi 65W USB-C", "Accesorios", new BigDecimal("59"), 50, 19, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283243/techstore/productos/tptqy0iyesqri08kaof4.webp", "Carga rápida 65W con cable USB-C incluido."),
            new ProductoSeed("Monitor Xiaomi G27Qi 27\" 2K 200Hz", "Computadoras", new BigDecimal("729"), 6, 15, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283375/techstore/productos/zps0k73wpbdt05w0yuh9.webp", "27\" QHD IPS, 200Hz, 1ms, HDR400, FreeSync Gaming."),
            new ProductoSeed("Parlante JBL Flip 7 Bluetooth", "Audio", new BigDecimal("329"), 18, 28, "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283548/techstore/productos/bfowowyk2orloct81aqe.webp", "Bluetooth 5.4, resistente al agua IP67, 14h de reproducción.")
    );

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
        if (p.getVendidos() != null && p.getVendidos() > 0) {
            throw new IllegalArgumentException("No se puede eliminar un producto con ventas registradas");
        }
        productos.delete(p);
    }

    public void verificarRolPermitido(Integer usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String rol = u.getRol() != null ? u.getRol().getNombreRol() : null;
        if (!ROLES_PERMITIDOS.contains(rol)) {
            throw new IllegalArgumentException("No tiene permisos para gestionar productos");
        }
    }

    /** Registra los productos iniciales (si no existen) subiendo sus imágenes a Cloudinary. */
    @Transactional
    public List<ProductoDTO> registrarProductosIniciales() {
        List<ProductoDTO> creados = new ArrayList<>();
        for (ProductoSeed s : PRODUCTOS_INICIALES) {
            boolean existe = productos.findAll().stream().anyMatch(p -> s.nombre().equals(p.getNombre()));
            if (existe) continue;
            byte[] bytes = leerImagen(s.imagen());
            String imagenFinal;
            try {
                Map<String, Object> upload = cloudinaryService.subir(s.imagen(), bytes);
                imagenFinal = String.valueOf(upload.get("secure_url"));
            } catch (Exception e) {
                imagenFinal = "/productos/" + s.imagen();
            }
            Producto p = new Producto();
            p.setNombre(s.nombre());
            p.setCategoria(s.categoria());
            p.setPrecio(s.precio());
            p.setStock(s.stock());
            p.setVendidos(s.vendidos());
            p.setImagen(imagenFinal);
            p.setDescripcion(s.descripcion());
            p.setActivo(true);
            creados.add(mapear(productos.save(p)));
        }
        return creados;
    }

    private byte[] leerImagen(String nombreArchivo) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("static/productos/" + nombreArchivo)) {
            if (is == null) throw new IllegalArgumentException("Imagen no encontrada: " + nombreArchivo);
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer la imagen: " + nombreArchivo, e);
        }
    }

    private ProductoDTO mapear(Producto p) {
        return new ProductoDTO(p.getIdProducto(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getStock(), p.getVendidos(), p.getImagen(), p.getDescripcion(), p.getActivo());
    }
}
