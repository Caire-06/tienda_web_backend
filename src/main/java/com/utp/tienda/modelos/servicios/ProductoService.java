package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.EspecificacionTecnica;
import com.utp.tienda.modelos.entidades.InventarioInicial;
import com.utp.tienda.modelos.entidades.Producto;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.EspecificacionTecnicaRepository;
import com.utp.tienda.modelos.repositorios.InventarioInicialRepository;
import com.utp.tienda.modelos.repositorios.ProductoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.ActualizarProductoDTO;
import com.utp.tienda.vistas.dto.CrearProductoDTO;
import com.utp.tienda.vistas.dto.EspecificacionDTO;
import com.utp.tienda.vistas.dto.InventarioInicialDTO;
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
    private final InventarioInicialRepository inventariosIniciales;

    public ProductoService(ProductoRepository productos,
                           UsuarioRepository usuarios,
                           CloudinaryService cloudinaryService,
                           EspecificacionTecnicaRepository especificaciones,
                           InventarioInicialRepository inventariosIniciales) {
        this.productos = productos;
        this.usuarios = usuarios;
        this.cloudinaryService = cloudinaryService;
        this.especificaciones = especificaciones;
        this.inventariosIniciales = inventariosIniciales;
    }

    /** Datos de los productos iniciales (información actual de la tienda) + imagen local a subir a Cloudinary. */
    private record ProductoSeed(String nombre, String categoria, String marca, BigDecimal precio, Integer stock,
                                BigDecimal costoInicial, String imagen, String descripcion,
                                List<EspecificacionDTO> especificaciones) {}

    private static List<EspecificacionDTO> specs(EspecificacionDTO... valores) {
        return List.of(valores);
    }

    private static EspecificacionDTO spec(String caracteristica, String valor) {
        return new EspecificacionDTO(caracteristica, valor);
    }

    private static final List<ProductoSeed> PRODUCTOS_INICIALES = List.of(
            new ProductoSeed("Mouse Logitech G502 HERO", "Periféricos", "Logitech",
                    new BigDecimal("229"), 20, new BigDecimal("170"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283457/techstore/productos/x0tf8pobbhkai8ja3rki.webp",
                    "Mouse gamer alámbrico 25K DPI, 11 botones programables.",
                    specs(spec("Sensor", "HERO 25K DPI"), spec("Conectividad", "USB-C alámbrico"),
                            spec("Botones", "11 programables"), spec("Peso", "121 g"))),
            new ProductoSeed("Audífonos Sony WH-1000XM4", "Audio", "Sony",
                    new BigDecimal("1049"), 12, new BigDecimal("800"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794282995/techstore/productos/wq2rgb2dqa3brrbeqxjj.webp",
                    "Audífonos bluetooth con noise cancelling, 30h de batería.",
                    specs(spec("Tipo de Diseño", "Over-Ear (Diadema)"),
                            spec("Cancelación de Ruido", "Sí (ANC Activa)"),
                            spec("Autonomía", "30 horas"),
                            spec("Conectividad", "Bluetooth 5.3 y Cable 3.5mm"),
                            spec("Micrófono", "Integrado con reducción de ruido"))),
            new ProductoSeed("Teclado Redragon Fizz Pro K616 RGB", "Periféricos", "Redragon",
                    new BigDecimal("215"), 5, new BigDecimal("150"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283650/techstore/productos/otje91i9tsezcdkaso9o.webp",
                    "Teclado mecánico inalámbrico, RGB, 87 teclas.",
                    specs(spec("Tipo de Switch", "Mecánico rojo"), spec("Conectividad", "Inalámbrico 2.4 GHz"),
                            spec("Distribución", "87 teclas TKL"), spec("Iluminación", "RGB"))),
            new ProductoSeed("Laptop Lenovo IdeaPad Slim 3 Ryzen 7 / 16GB / 512GB", "Computadoras", "Lenovo",
                    new BigDecimal("2599"), 8, new BigDecimal("2000"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283309/techstore/productos/pqnx12wxjenwsjti2t6k.webp",
                    "15.6\" FHD, Ryzen 7 serie 5000, 16GB RAM, 512GB SSD.",
                    specs(spec("Procesador", "AMD Ryzen 7 serie 5000"),
                            spec("Memoria RAM", "16 GB DDR4"),
                            spec("Almacenamiento", "512 GB SSD"),
                            spec("Pantalla", "15.6\" FHD"),
                            spec("Sistema Operativo", "Windows 11 Home"))),
            new ProductoSeed("Samsung Galaxy A55 5G 8GB / 128GB", "Smartphones", "Samsung",
                    new BigDecimal("1481"), 15, new BigDecimal("1150"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283605/techstore/productos/vdjvpqg0bp7c3155bkns.webp",
                    "Pantalla 6.6\" Super AMOLED 120Hz, cámara 50MP, batería 5000mAh.",
                    specs(spec("Memoria RAM", "8 GB"),
                            spec("Almacenamiento", "128 GB"),
                            spec("Batería", "5000 mAh"),
                            spec("Pantalla", "6.6\" Super AMOLED 120Hz"),
                            spec("Procesador", "Exynos 1480"),
                            spec("Conectividad", "5G Dual SIM"))),
            new ProductoSeed("Kit Cargador Xiaomi 65W USB-C", "Accesorios", "Xiaomi",
                    new BigDecimal("59"), 50, new BigDecimal("35"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283243/techstore/productos/tptqy0iyesqri08kaof4.webp",
                    "Carga rápida 65W con cable USB-C incluido.",
                    specs(spec("Potencia", "65 W"), spec("Conector", "USB-C"),
                            spec("Contenido", "Cargador y cable USB-C"))),
            new ProductoSeed("Monitor Xiaomi G27Qi 27\" 2K 200Hz", "Computadoras", "Xiaomi",
                    new BigDecimal("729"), 6, new BigDecimal("550"),
                    "https://res.cloudinary.com/svlpcuhv/image/upload/v1794283375/techstore/productos/zps0k73wpbdt05w0yuh9.webp",
                    "27\" QHD IPS, 200Hz, 1ms, HDR400, FreeSync Gaming.",
                    specs(spec("Pantalla", "27\" QHD IPS"), spec("Tasa de Refresco", "200 Hz"),
                            spec("Tiempo de Respuesta", "1 ms"), spec("Panel", "IPS HDR400"))),
            new ProductoSeed("Parlante JBL Flip 7 Bluetooth", "Audio", "JBL",
                    new BigDecimal("329"), 18, new BigDecimal("245"),
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
    public org.springframework.data.domain.Page<ProductoDTO> listarPaginado(String categoria, String busqueda, org.springframework.data.domain.Pageable pageable) {
        return productos.buscarPaginado(categoria, busqueda, pageable)
                .map(this::mapear);
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
        p.setVendidos(0);
        p.setImagen(dto.imagen());
        p.setDescripcion(dto.descripcion());
        p.setActivo(dto.activo() != null ? dto.activo() : true);
        p.setMarca(marcaNormalizada(dto.marca()));
        p.setFechaCreacion(LocalDateTime.now());
        p.setFechaActualizacion(LocalDateTime.now());
        p.setUsuarioRegistro(usuario);
        sincronizarEspecificaciones(p, dto.especificaciones(), usuario);
        Producto guardado = productos.save(p);
        registrarInventarioInicialSiAplica(guardado, dto.costoUnitarioInicial(), usuario);
        return mapear(guardado);
    }

    @Transactional
    public ProductoDTO actualizar(Integer usuarioId, Integer id, ActualizarProductoDTO dto) {
        verificarRolPermitido(usuarioId);
        Producto p = productos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (dto.nombre() != null) p.setNombre(dto.nombre());
        if (dto.categoria() != null) p.setCategoria(dto.categoria());
        if (dto.precio() != null) p.setPrecio(dto.precio());
        if (dto.stock() != null && !java.util.Objects.equals(dto.stock(), p.getStock())) throw new IllegalArgumentException("El stock se modifica mediante entradas y ventas");
        if (dto.imagen() != null) p.setImagen(dto.imagen());
        if (dto.descripcion() != null) p.setDescripcion(dto.descripcion());
        if (dto.vendidos() != null && !java.util.Objects.equals(dto.vendidos(), p.getVendidos())) throw new IllegalArgumentException("Las unidades vendidas se calculan mediante pedidos");
        if (dto.activo() != null) p.setActivo(dto.activo());
        if (dto.marca() != null) p.setMarca(marcaNormalizada(dto.marca()));
        p.setFechaActualizacion(LocalDateTime.now());
        p.setUsuarioRegistro(nombreUsuario(usuarioId));
        // Lista NN = reemplazo completo de la ficha técnica; null = no tocarla.
        if (dto.especificaciones() != null) {
            sincronizarEspecificaciones(p, dto.especificaciones(), nombreUsuario(usuarioId));
        }
        if (dto.costoUnitarioInicial() != null) {
            throw new IllegalArgumentException("El costo inicial solo se registra al crear el producto");
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

    /**
     * Regulariza el inventario inicial de un producto ya existente.
     * Se toma su stock físico actual como cantidad del evento y se fecha ahora,
     * sin duplicar existencias ni volver a contabilizar compras y ventas históricas.
     * Si el evento ya existe, solo se corrige el costo (nunca la cantidad ni la fecha).
     */
    @Transactional
    public ProductoDTO regularizarInventarioInicial(Integer usuarioId, Integer productoId,
                                                    BigDecimal costoUnitario) {
        throw new IllegalArgumentException("La regularización manual está deshabilitada");
    }

    /** Eventos de inventario inicial registrados (para auditoría del Kardex). */
    @Transactional(readOnly = true)
    public List<InventarioInicialDTO> inventariosIniciales() {
        return inventariosIniciales.findByActivoTrue().stream()
                .map(e -> new InventarioInicialDTO(
                        e.getProductoId(),
                        e.getProducto() != null ? e.getProducto().getNombre() : "",
                        e.getCantidad(),
                        e.getCostoUnitario(),
                        e.getFecha() == null ? null : e.getFecha().toString(),
                        e.getUsuarioRegistro()
                ))
                .toList();
    }

    /** Productos activos con stock físico que aún no tienen costo unitario inicial. */
    @Transactional(readOnly = true)
    public List<ProductoDTO> pendientesRegularizacion() {
        List<Integer> conEvento = inventariosIniciales.findByActivoTrue().stream()
                .map(InventarioInicial::getProductoId)
                .toList();
        return productos.findAllByOrderByNombreAsc().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo())
                        && p.getStock() != null && p.getStock() > 0
                        && !conEvento.contains(p.getIdProducto()))
                .map(this::mapear)
                .toList();
    }

    /** Registra los productos iniciales (si no existen) subiendo sus imágenes a Cloudinary. */
    @Transactional
    public List<ProductoDTO> registrarProductosIniciales() {
        List<ProductoDTO> creados = new ArrayList<>();
        for (ProductoSeed s : PRODUCTOS_INICIALES) {
            boolean existe = productos.findAll().stream().anyMatch(p -> s.nombre().equals(p.getNombre()));
            if (existe) continue;
            String imagenFinal = s.imagen(); // URL existente del catálogo de prueba.
            Producto p = new Producto();
            p.setNombre(s.nombre());
            p.setCategoria(s.categoria());
            p.setMarca(s.marca());
            p.setPrecio(s.precio());
            p.setStock(s.stock());
            p.setVendidos(0);
            p.setImagen(imagenFinal);
            p.setDescripcion(s.descripcion());
            p.setActivo(true);
            p.setFechaCreacion(LocalDateTime.now());
            p.setFechaActualizacion(LocalDateTime.now());
            p.setUsuarioRegistro(USUARIO_SISTEMA);
            sincronizarEspecificaciones(p, s.especificaciones(), USUARIO_SISTEMA);
            Producto guardado = productos.save(p);
            registrarInventarioInicialSiAplica(guardado, s.costoInicial(), USUARIO_SISTEMA);
            creados.add(mapear(guardado));
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

    /** Al dar de alta un producto con stock físico, genera su evento de inventario inicial. */
    private void registrarInventarioInicialSiAplica(Producto p, BigDecimal costo, String usuario) {
        int stock = p.getStock() != null ? p.getStock() : 0;
        if (stock <= 0) return;
        if (costo == null || costo.signum() <= 0 || costo.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("El costo inicial debe ser positivo y tener hasta dos decimales");
        if (inventariosIniciales.findByProducto_IdProducto(p.getIdProducto()).isPresent()) return;
        inventariosIniciales.save(new InventarioInicial(p, stock, costo, LocalDateTime.now(), usuario));
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
        List<EspecificacionDTO> ficha = p.getIdProducto() == null
                ? List.of()
                : especificaciones.findByIdProductoAndActivoTrue(p.getIdProducto()).stream()
                        .map(e -> new EspecificacionDTO(e.getCaracteristica(), e.getValor()))
                        .toList();
        InventarioInicial inicial = p.getIdProducto() == null
                ? null
                : inventariosIniciales.findByProducto_IdProducto(p.getIdProducto()).orElse(null);
        return new ProductoDTO(
                p.getIdProducto(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getStock(), p.getVendidos(), p.getImagen(), p.getDescripcion(),
                p.getActivo(), p.getMarca(),
                p.getFechaCreacion() == null ? null : p.getFechaCreacion().toString(),
                ficha,
                inicial != null ? inicial.getCostoUnitario() : null,
                inicial != null && inicial.getFecha() != null ? inicial.getFecha().toString() : null
        );
    }
}
