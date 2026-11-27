package com.utp.tienda.configuracion;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.*;
import com.utp.tienda.modelos.servicios.PasswordEncryptionStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inicializador automático de datos mediante Hibernate (ORM puro).
 * Se ejecuta al arrancar Spring Boot, garantizando que los catálogos en 3FN,
 * usuarios iniciales, productos y ventas de prueba existan en la base de datos PostgreSQL.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final PaisRepository paisRepository;
    private final DepartamentoRepository departamentoRepository;
    private final ProvinciaRepository provinciaRepository;
    private final DistritoRepository distritoRepository;
    private final SexoRepository sexoRepository;
    private final EstadoCivilRepository estadoCivilRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DatosPersonalesRepository datosPersonalesRepository;
    private final CorreoElectronicoRepository correoRepository;
    private final TelefonoRepository telefonoRepository;
    private final DireccionRepository direccionRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final com.utp.tienda.modelos.servicios.ProductoService productoService;
    private final PasswordEncryptionStrategy encryptionStrategy;

    @Value("${app.seed.admin-password:}")
    private String seedAdminPassword;

    @Value("${app.seed.almacen-password:}")
    private String seedAlmacenPassword;

    public DataInitializer(PaisRepository paisRepository,
                           DepartamentoRepository departamentoRepository,
                           ProvinciaRepository provinciaRepository,
                           DistritoRepository distritoRepository,
                           SexoRepository sexoRepository,
                           EstadoCivilRepository estadoCivilRepository,
                           RolRepository rolRepository,
                           PermisoRepository permisoRepository,
                           UsuarioRepository usuarioRepository,
                           DatosPersonalesRepository datosPersonalesRepository,
                           CorreoElectronicoRepository correoRepository,
                           TelefonoRepository telefonoRepository,
                           DireccionRepository direccionRepository,
                           ProductoRepository productoRepository,
                           PedidoRepository pedidoRepository,
                           com.utp.tienda.modelos.servicios.ProductoService productoService,
                           @Qualifier("sha256Strategy") PasswordEncryptionStrategy encryptionStrategy) {
        this.paisRepository = paisRepository;
        this.departamentoRepository = departamentoRepository;
        this.provinciaRepository = provinciaRepository;
        this.distritoRepository = distritoRepository;
        this.sexoRepository = sexoRepository;
        this.estadoCivilRepository = estadoCivilRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.datosPersonalesRepository = datosPersonalesRepository;
        this.correoRepository = correoRepository;
        this.telefonoRepository = telefonoRepository;
        this.direccionRepository = direccionRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoService = productoService;
        this.encryptionStrategy = encryptionStrategy;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println(">>> [Hibernate] Verificando e inicializando catálogos y datos iniciales en PostgreSQL...");

        // 1. Jerarquía Geográfica en 3FN
        Pais peru = paisRepository.findByNombrePais("Perú")
                .orElseGet(() -> paisRepository.save(new Pais(null, "Perú")));

        Departamento limaDep = departamentoRepository.findByNombreDepartamento("Lima")
                .orElseGet(() -> {
                    Departamento dep = new Departamento();
                    dep.setNombreDepartamento("Lima");
                    dep.setPais(peru);
                    return departamentoRepository.save(dep);
                });

        Provincia limaProv = provinciaRepository.findByNombreProvincia("Lima")
                .orElseGet(() -> {
                    Provincia prov = new Provincia();
                    prov.setNombreProvincia("Lima");
                    prov.setDepartamento(limaDep);
                    return provinciaRepository.save(prov);
                });

        Distrito sjl = distritoRepository.findByNombreDistrito("San Juan de Lurigancho")
                .orElseGet(() -> {
                    Distrito dist = new Distrito();
                    dist.setNombreDistrito("San Juan de Lurigancho");
                    dist.setProvincia(limaProv);
                    return distritoRepository.save(dist);
                });

        // 2. Catálogos Personales
        Sexo masculino = sexoRepository.findByDescripcion("Masculino")
                .orElseGet(() -> sexoRepository.save(new Sexo(null, "Masculino")));
        sexoRepository.findByDescripcion("Femenino")
                .orElseGet(() -> sexoRepository.save(new Sexo(null, "Femenino")));
        sexoRepository.findByDescripcion("No Especifica")
                .orElseGet(() -> sexoRepository.save(new Sexo(null, "No Especifica")));

        EstadoCivil soltero = estadoCivilRepository.findByDescripcion("Soltero(a)")
                .orElseGet(() -> estadoCivilRepository.save(new EstadoCivil(null, "Soltero(a)")));
        estadoCivilRepository.findByDescripcion("Casado(a)")
                .orElseGet(() -> estadoCivilRepository.save(new EstadoCivil(null, "Casado(a)")));

        // 3. Roles y Permisos (3 roles del sistema: ADMINISTRADOR, ALMACEN, CLIENTE)
        Rol rolAdmin = rolRepository.findByNombreRol("ADMINISTRADOR")
                .orElseGet(() -> rolRepository.save(new Rol(null, "ADMINISTRADOR", "Acceso total y configuración del sistema")));
        Rol rolAlmacen = rolRepository.findByNombreRol("ALMACEN")
                .orElseGet(() -> rolRepository.save(new Rol(null, "ALMACEN", "Gestión física de inventario, stock, kardex y despachos")));
        rolRepository.findByNombreRol("CLIENTE")
                .orElseGet(() -> rolRepository.save(new Rol(null, "CLIENTE", "Acceso a catálogo y compras")));

        // Consolidar cualquier cuenta previa con rol OPERADOR hacia ALMACEN
        usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && "OPERADOR".equalsIgnoreCase(u.getRol().getNombreRol()))
                .forEach(u -> {
                    u.setRol(rolAlmacen);
                    usuarioRepository.save(u);
                });

        Permiso permisoTodo = permisoRepository.findByNombrePermiso("TODO")
                .orElseGet(() -> permisoRepository.save(new Permiso(null, "TODO", "Acceso total a todos los módulos")));

        // 4. Usuario Administrador Inicial (requiere SEED_ADMIN_PASSWORD en .env o Render)
        Usuario adminGuardado = null;
        if (!usuarioRepository.existsByNombreUsuario("admin")) {
            if (seedAdminPassword != null && !seedAdminPassword.isBlank()) {
                Usuario admin = new Usuario();
                admin.setNombreUsuario("admin");
                admin.setContrasena(encryptionStrategy.encriptar(seedAdminPassword.trim()));
                admin.setRol(rolAdmin);
                admin.setPermiso(permisoTodo);
                admin.setIntentosFallidos(0);
                admin.setBloqueado(false);
                admin.setCorreoVerificado(true);

                adminGuardado = usuarioRepository.save(admin);

                // Datos Personales en 3FN
                DatosPersonales datos = new DatosPersonales();
                datos.setUsuario(adminGuardado);
                datos.setNombres("Gustavo");
                datos.setApellidos("Asto Aiquipa");
                datos.setDni("72345678");
                datos.setSexo(masculino);
                datos.setEstadoCivil(soltero);

                DatosPersonales datosGuardados = datosPersonalesRepository.save(datos);

                // Correo 1:N
                CorreoElectronico correo = new CorreoElectronico();
                correo.setDatosPersonales(datosGuardados);
                correo.setCorreoElectronico("admin@techstore.com");
                correo.setTipoCorreo("CORPORATIVO");
                correoRepository.save(correo);

                // Teléfono 1:N
                Telefono telefono = new Telefono();
                telefono.setDatosPersonales(datosGuardados);
                telefono.setNumeroTelefono("987654321");
                telefono.setTipoTelefono("MOVIL");
                telefonoRepository.save(telefono);

                // Dirección en 3FN
                Direccion direccion = new Direccion();
                direccion.setDatosPersonales(datosGuardados);
                direccion.setDistrito(sjl);
                direccion.setCalleAvenida("Av. Las Flores 123");
                direccion.setTipoDireccion("DOMICILIO");
                direccion.setNumero("123");
                direccion.setReferencia("Cerca a la estación del tren");
                direccionRepository.save(direccion);

                System.out.println(">>> [Hibernate] Usuario 'admin' inicializado exitosamente.");
            } else {
                System.out.println(">>> [Hibernate] SEED_ADMIN_PASSWORD no definido: se omite creación automática de 'admin'.");
            }
        } else {
            adminGuardado = usuarioRepository.findByNombreUsuario("admin").orElse(null);
        }

        // 5. Usuario Almacén Inicial (requiere SEED_ALMACEN_PASSWORD en .env o Render)
        if (!usuarioRepository.existsByNombreUsuario("almacen")) {
            if (seedAlmacenPassword != null && !seedAlmacenPassword.isBlank()) {
                Usuario userAlmacen = new Usuario();
                userAlmacen.setNombreUsuario("almacen");
                userAlmacen.setContrasena(encryptionStrategy.encriptar(seedAlmacenPassword.trim()));
                userAlmacen.setRol(rolAlmacen);
                userAlmacen.setPermiso(permisoTodo);
                userAlmacen.setIntentosFallidos(0);
                userAlmacen.setBloqueado(false);
                userAlmacen.setCorreoVerificado(true);

                Usuario almacenGuardado = usuarioRepository.save(userAlmacen);

                DatosPersonales datosAlmacen = new DatosPersonales();
                datosAlmacen.setUsuario(almacenGuardado);
                datosAlmacen.setNombres("Carlos");
                datosAlmacen.setApellidos("Operador Almacén");
                datosAlmacen.setDni("71234567");
                datosAlmacen.setSexo(masculino);
                datosAlmacen.setEstadoCivil(soltero);

                DatosPersonales datosAlmacenGuardados = datosPersonalesRepository.save(datosAlmacen);

                CorreoElectronico correoAlmacen = new CorreoElectronico();
                correoAlmacen.setDatosPersonales(datosAlmacenGuardados);
                correoAlmacen.setCorreoElectronico("almacen@techstore.com");
                correoAlmacen.setTipoCorreo("CORPORATIVO");
                correoRepository.save(correoAlmacen);

                Telefono telefonoAlmacen = new Telefono();
                telefonoAlmacen.setDatosPersonales(datosAlmacenGuardados);
                telefonoAlmacen.setNumeroTelefono("987654322");
                telefonoAlmacen.setTipoTelefono("MOVIL");
                telefonoRepository.save(telefonoAlmacen);

                Direccion dirAlmacen = new Direccion();
                dirAlmacen.setDatosPersonales(datosAlmacenGuardados);
                dirAlmacen.setDistrito(sjl);
                dirAlmacen.setCalleAvenida("Av. Las Flores 124");
                dirAlmacen.setTipoDireccion("DOMICILIO");
                dirAlmacen.setNumero("124");
                dirAlmacen.setReferencia("Almacén Central TechStore");
                direccionRepository.save(dirAlmacen);

                System.out.println(">>> [Hibernate] Usuario 'almacen' inicializado exitosamente.");
            } else {
                System.out.println(">>> [Hibernate] SEED_ALMACEN_PASSWORD no definido: se omite creación automática de 'almacen'.");
            }
        }

        // 6. Catálogo de Productos Inicial en Base de Datos
        boolean catalogoNuevo = productoRepository.count() == 0;
        if (catalogoNuevo) {
            productoService.registrarProductosIniciales();
        }

        // 7. Ventas / Pedidos Iniciales en Base de Datos
        if (catalogoNuevo && pedidoRepository.count() == 0 && adminGuardado != null) {
            {
                Producto p1 = productoRepository.findByNombre("Mouse Logitech G502 HERO").orElseThrow();
                Producto p2 = productoRepository.findByNombre("Audífonos Sony WH-1000XM4").orElseThrow();
                Producto p5 = productoRepository.findByNombre("Samsung Galaxy A55 5G 8GB / 128GB").orElseThrow();

                // Pedido 1
                crearPedidoDemo(adminGuardado, "VTA-001", "María Torres", "73214569", "Boleta",
                        LocalDateTime.now(), "Entregado", "delivery_propio", "tarjeta",
                        List.of(new DetalleDemo(p1.getIdProducto(), p1.getNombre(), p1.getPrecio(), 2, p1.getImagen())));

                // Pedido 2
                crearPedidoDemo(adminGuardado, "VTA-002", "Tecnología Global S.A.C.", "20601234567", "Factura",
                        LocalDateTime.now(), "Pendiente", "delivery_propio", "transferencia",
                        List.of(new DetalleDemo(p5.getIdProducto(), p5.getNombre(), p5.getPrecio(), 1, p5.getImagen())));

                // Pedido 3
                crearPedidoDemo(adminGuardado, "VTA-003", "José Ramírez", "45678912", "Boleta",
                        LocalDateTime.now(), "Pendiente", "recojo", "yape",
                        List.of(new DetalleDemo(p2.getIdProducto(), p2.getNombre(), p2.getPrecio(), 1, p2.getImagen())));

                System.out.println(">>> [Hibernate] Ventas y Comprobantes de prueba inicializados en PostgreSQL.");
            }
        }
    }

    private record DetalleDemo(Integer productoId, String nombre, BigDecimal precio, int cantidad, String imagen) {}

    private void crearPedidoDemo(Usuario usuario, String numero, String cliente, String doc, String tipoComp,
                                 LocalDateTime fecha, String estado, String tipoEntrega, String metodoPago,
                                 List<DetalleDemo> items) {
        Pedido p = new Pedido();
        p.setNumero(numero);
        p.setFecha(fecha);
        p.setUsuario(usuario);
        p.setEstado(estado);
        p.setEstadoPago("Pagado");
        if ("Entregado".equals(estado)) {
            p.setFechaProcesando(fecha);
            p.setFechaEnvio(fecha);
            p.setFechaEntrega(fecha);
        }
        p.setTipoEntrega(tipoEntrega);
        p.setMetodoPago(metodoPago);
        p.setCorreoContacto("cliente@techstore.com");
        p.setCelularContacto("987654321");
        p.setDireccionEnvio("Av. Las Flores 123");
        p.setDistritoEnvio("San Juan de Lurigancho");

        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detalles = new ArrayList<>();
        for (DetalleDemo item : items) {
            DetallePedido d = new DetallePedido();
            d.setPedido(p);
            Producto producto = productoRepository.findByIdConBloqueo(item.productoId()).orElseThrow();
            if (producto.getStock() < item.cantidad()) throw new IllegalStateException("Stock demo insuficiente");
            producto.setStock(producto.getStock() - item.cantidad());
            producto.setVendidos(producto.getVendidos() + item.cantidad());
            d.setProducto(producto);
            d.setNombre(item.nombre());
            d.setPrecioUnitario(item.precio());
            d.setCantidad(item.cantidad());
            d.setImagen(item.imagen());
            detalles.add(d);
            total = total.add(item.precio().multiply(BigDecimal.valueOf(item.cantidad())));
        }
        p.setTotal(total);
        p.setDetalles(detalles);

        ComprobantePedido comp = new ComprobantePedido();
        comp.setPedido(p);
        comp.setTipo(tipoComp);
        comp.setDocumento(doc);
        comp.setNombreRazonSocial(cliente);
        comp.setDireccionFiscal("Av. Principal 456, Lima");
        BigDecimal subtotal = total.divide(BigDecimal.valueOf(1.18), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);
        comp.setSubtotal(subtotal);
        comp.setIgv(igv);
        comp.setTotal(total);
        p.setComprobante(comp);

        pedidoRepository.save(p);
    }
}
