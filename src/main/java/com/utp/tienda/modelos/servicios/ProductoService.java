package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.EspecificacionTecnica;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.EspecificacionTecnicaRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.ActualizarProductoDTO;
import com.utp.tienda.vistas.dto.CrearProductoDTO;
import com.utp.tienda.vistas.dto.EspecificacionDTO;
import com.utp.tienda.vistas.dto.ProductoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {
    private static final List<String> ROLES_PERMITIDOS = List.of("ADMINISTRADOR", "ALMACEN");
    private static final String MARCA_POR_DEFECTO = "Genérico";
    private static final String USUARIO_SISTEMA = "SISTEMA";

    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final CloudinaryService cloudinaryService;
    private final EspecificacionTecnicaRepository especificaciones;

    public ProductoService(ProductoRepository productos,
                           UsuarioRepository usuarios,
                           CloudinaryService cloudinaryService,
                           EspecificacionTecnicaRepository especificaciones) {
        this.productos = productos;
        this.usuarios = usuarios;
        this.cloudinaryService = cloudinaryService;
        this.especificaciones = especificaciones;
    }

    /** Datos de los productos iniciales (información actual de la tienda) + imagen local a subir a Cloudinary. */
    private record ProductoSeed(String nombre, String categoria, String marca, BigDecimal precio, Integer stock,
                                Integer vendidos, String imagen, String descripcion,
                                List<EspecificacionDTO> especificaciones) {}

    private static List<EspecificacionDTO> specs(EspecificacionDTO... valores) {
        return List.of(valores);
    }

    private static EspecificacionDTO spec(String caracteristica, String valor) {
        return new EspecificacionDTO(caracteristica, valor);
    }

    private static final List<ProductoSeed> PRODUCTOS_INICIALES = List.of(
            new ProductoSeed("Mouse Logitech G502 HERO", "Periféricos", "Logitech",
                    new BigDecimal("229"), 20, 38,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283457/techstore/productos/x0tf8pobbhkai8ja3rki.webp",
                    "Mouse gamer alámbrico 25K DPI, 11 botones programables.",
                    specs(spec("Sensor", "HERO 25K DPI"), spec("Conectividad", "USB-C alámbrico"),
                            spec("Botones", "11 programables"), spec("Peso", "121 g"))),
            new ProductoSeed("Audífonos Sony WH-1000XM4", "Audio", "Sony",
                    new BigDecimal("1049"), 12, 45,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794282995/techstore/productos/wq2rgb2dqa3brrbeqxjj.webp",
                    "Audífonos bluetooth con noise cancelling, 30h de batería.",
                    specs(spec("Tipo de Diseño", "Over-Ear (Diadema)"),
                            spec("Cancelación de Ruido", "Sí (ANC Activa)"),
                            spec("Autonomía", "30 horas"),
                            spec("Conectividad", "Bluetooth 5.3 y Cable 3.5mm"),
                            spec("Micrófono", "Integrado con reducción de ruido"))),
            new ProductoSeed("Teclado Redragon Fizz Pro K616 RGB", "Periféricos", "Redragon",
                    new BigDecimal("215"), 0, 25,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283650/techstore/productos/otje91i9tsezcdkaso9o.webp",
                    "Teclado mecánico inalámbrico, RGB, 87 teclas.",
                    specs(spec("Tipo de Switch", "Mecánico rojo"), spec("Conectividad", "Inalámbrico 2.4 GHz"),
                            spec("Distribución", "87 teclas TKL"), spec("Iluminación", "RGB"))),
            new ProductoSeed("Laptop Lenovo IdeaPad Slim 3 Ryzen 7 / 16GB / 512GB", "Computadoras", "Lenovo",
                    new BigDecimal("2599"), 8, 12,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283309/techstore/productos/pqnx12wxjenwsjti2t6k.webp",
                    "15.6\" FHD, Ryzen 7 serie 5000, 16GB RAM, 512GB SSD.",
                    specs(spec("Procesador", "AMD Ryzen 7 serie 5000"),
                            spec("Memoria RAM", "16 GB DDR4"),
                            spec("Almacenamiento", "512 GB SSD"),
                            spec("Pantalla", "15.6\" FHD"),
                            spec("Sistema Operativo", "Windows 11 Home"))),
            new ProductoSeed("Samsung Galaxy A55 5G 8GB / 128GB", "Smartphones", "Samsung",
                    new BigDecimal("1481"), 15, 30,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283605/techstore/productos/vdjvpqg0bp7c3155bkns.webp",
                    "Pantalla 6.6\" Super AMOLED 120Hz, cámara 50MP, batería 5000mAh.",
                    specs(spec("Memoria RAM", "8 GB"),
                            spec("Almacenamiento", "128 GB"),
                            spec("Batería", "5000 mAh"),
                            spec("Pantalla", "6.6\" Super AMOLED 120Hz"),
                            spec("Procesador", "Exynos 1480"),
                            spec("Conectividad", "5G Dual SIM"))),
            new ProductoSeed("Kit Cargador Xiaomi 65W USB-C", "Accesorios", "Xiaomi",
                    new BigDecimal("59"), 50, 19,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283243/techstore/productos/tptqy0iyesqri08kaof4.webp",
                    "Carga rápida 65W con cable USB-C incluido.",
                    specs(spec("Potencia", "65 W"), spec("Conector", "USB-C"),
                            spec("Contenido", "Cargador y cable USB-C"))),
            new ProductoSeed("Monitor Xiaomi G27Qi 27\" 2K 200Hz", "Computadoras", "Xiaomi",
                    new BigDecimal("729"), 6, 15,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283375/techstore/productos/zps0k73wpbdt05w0yuh9.webp",
                    "27\" QHD IPS, 200Hz, 1ms, HDR400, FreeSync Gaming.",
                    specs(spec("Pantalla", "27\" QHD IPS"), spec("Tasa de Refresco", "200 Hz"),
                            spec("Tiempo de Respuesta", "1 ms"), spec("Panel", "IPS HDR400"))),
            new ProductoSeed("Parlante JBL Flip 7 Bluetooth", "Audio", "JBL",
                    new BigDecimal("329"), 18, 28,
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283548/techstore/productos/bfowowyk2orloct81aqe.webp",
                    "Bluetooth 5.4, resistente al agua IP67, 14h de reproducción.",
                    specs(spec("Conectividad", "Bluetooth 5.4"), spec("Autonomía", "14 horas"),
                            spec("Resistencia", "IP67"), spec("Tipo de Diseño", "Portátil")))
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
        String usuario = nombreUsuario(usuarioId);
        Producto p = new Producto();
        p.setNombre(dto.nombre());
        p.setCategoria(dto.categoria());
        p.setPrecio(dto.precio());
        p.setStock(dto.stock());
        p.setVendidos(dto.vendidos() != null ? dto.vendidos() : 0);
        p.setImagen(dto.imagen());
        p.setDescripcion(dto.descripcion());
        p.setActivo(dto.activo() != null ? dto.activo() : true);
        p.setMarca(marcaNormalizada(dto.marca()));
        p.setFechaCreacion(LocalDateTime.now());
        p.setFechaActualizacion(LocalDateTime.now());
        p.setUsuarioRegistro(usuario);
        sincronizarEspecificaciones(p, dto.especificaciones(), usuario);
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
        if (dto.marca() != null) p.setMarca(marcaNormalizada(dto.marca()));
        p.setFechaActualizacion(LocalDateTime.now());
        p.setUsuarioRegistro(nombreUsuario(usuarioId));
        // Lista NN = reemplazo completo de la ficha técnica; null = no tocarla.
        if (dto.especificaciones() != null) {
            sincronizarEspecificaciones(p, dto.especificaciones(), nombreUsuario(usuarioId));
        }
        return mapear(productos.save(p));
    }

    @Transactional
    public ProductoDTO darDeBajaLogica(Integer id) {
        Producto p = productos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        p.setActivo(false);
        p.setFechaActualizacion(LocalDateTime.now());
        return mapear(productos.save(p));
    }

    public void eliminar(Integer id) {
        darDeBajaLogica(id);
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
            p.setMarca(s.marca());
            p.setPrecio(s.precio());
            p.setStock(s.stock());
            p.setVendidos(s.vendidos());
            p.setImagen(imagenFinal);
            p.setDescripcion(s.descripcion());
            p.setActivo(true);
            p.setFechaCreacion(LocalDateTime.now());
            p.setFechaActualizacion(LocalDateTime.now());
            p.setUsuarioRegistro(USUARIO_SISTEMA);
            sincronizarEspecificaciones(p, s.especificaciones(), USUARIO_SISTEMA);
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

    /** Reemplaza la ficha técnica completa del producto con auditoría (cascade + orphanRemoval). */
    private void sincronizarEspecificaciones(Producto p, List<EspecificacionDTO> dtos, String usuario) {
        if (dtos == null) return;
        List<EspecificacionDTO> limpias = dtos.stream()
                .filter(d -> d != null && d.caracteristica() != null && !d.caracteristica().isBlank()
                        && d.valor() != null && !d.valor().isBlank())
                .map(d -> new EspecificacionDTO(d.caracteristica().trim(), d.valor().trim()))
                .toList();
        p.getEspecificaciones().clear();
        for (EspecificacionDTO d : limpias) {
            EspecificacionTecnica e = new EspecificacionTecnica(p, d.caracteristica(), d.valor(), usuario);
            e.setFechaCreacion(LocalDateTime.now());
            e.setFechaActualizacion(LocalDateTime.now());
            p.getEspecificaciones().add(e);
        }
    }

    private String nombreUsuario(Integer usuarioId) {
        if (usuarioId == null) return USUARIO_SISTEMA;
        return usuarios.findById(usuarioId)
                .map(Usuario::getNombreUsuario)
                .filter(n -> n != null && !n.isBlank())
                .orElse(USUARIO_SISTEMA);
    }

    private String marcaNormalizada(String marca) {
        return (marca == null || marca.isBlank()) ? MARCA_POR_DEFECTO : marca.trim();
    }

    private ProductoDTO mapear(Producto p) {
        List<EspecificacionDTO> ficha = especificaciones.findByIdProductoAndActivoTrue(p.getIdProducto()).stream()
                .map(e -> new EspecificacionDTO(e.getCaracteristica(), e.getValor()))
                .toList();
        return new ProductoDTO(
                p.getIdProducto(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getStock(), p.getVendidos(), p.getImagen(), p.getDescripcion(),
                p.getActivo(), p.getMarca(),
                p.getFechaCreacion() == null ? null : p.getFechaCreacion().toString(),
                ficha
        );
    }
}
