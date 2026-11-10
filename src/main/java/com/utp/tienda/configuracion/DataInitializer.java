package com.utp.tienda.configuracion;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.*;
import com.utp.tienda.modelos.servicios.PasswordEncryptionStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador automático de datos mediante Hibernate (ORM puro).
 * Se ejecuta al arrancar Spring Boot, garantizando que los catálogos en 3FN
 * y el usuario administrador inicial existan sin necesidad de ejecutar scripts SQL manuales.
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
    private final PasswordEncryptionStrategy encryptionStrategy;

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
        this.encryptionStrategy = encryptionStrategy;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println(">>> [Hibernate] Verificando e inicializando catálogos y datos iniciales...");

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

        // 3. Roles y Permisos
        Rol rolAdmin = rolRepository.findByNombreRol("ADMINISTRADOR")
                .orElseGet(() -> rolRepository.save(new Rol(null, "ADMINISTRADOR", "Acceso total y configuración del sistema")));
        Rol rolAlmacen = rolRepository.findByNombreRol("ALMACEN")
                .orElseGet(() -> rolRepository.save(new Rol(null, "ALMACEN", "Gestión física de inventario, stock y kardex")));
        rolRepository.findByNombreRol("OPERADOR")
                .orElseGet(() -> rolRepository.save(new Rol(null, "OPERADOR", "Gestión operativa de compras y ventas")));
        rolRepository.findByNombreRol("CLIENTE")
                .orElseGet(() -> rolRepository.save(new Rol(null, "CLIENTE", "Acceso a catálogo y compras")));

        Permiso permisoTodo = permisoRepository.findByNombrePermiso("TODO")
                .orElseGet(() -> permisoRepository.save(new Permiso(null, "TODO", "Acceso total a todos los módulos")));

        // 4. Usuario Administrador Inicial
        if (!usuarioRepository.existsByNombreUsuario("admin")) {
            Usuario admin = new Usuario();
            admin.setNombreUsuario("admin");
            admin.setContrasena(encryptionStrategy.encriptar("admin123"));
            admin.setRol(rolAdmin);
            admin.setPermiso(permisoTodo);
            admin.setIntentosFallidos(0);
            admin.setBloqueado(false);

            Usuario adminGuardado = usuarioRepository.save(admin);

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

            System.out.println(">>> [Hibernate] Usuario 'admin' y perfil 3FN inicializados exitosamente.");
        } else {
            System.out.println(">>> [Hibernate] La base de datos ya cuenta con el usuario 'admin'.");
        }

        // 5. Usuario Almacén Inicial
        if (!usuarioRepository.existsByNombreUsuario("almacen")) {
            Usuario userAlmacen = new Usuario();
            userAlmacen.setNombreUsuario("almacen");
            userAlmacen.setContrasena(encryptionStrategy.encriptar("123"));
            userAlmacen.setRol(rolAlmacen);
            userAlmacen.setPermiso(permisoTodo);
            userAlmacen.setIntentosFallidos(0);
            userAlmacen.setBloqueado(false);

            Usuario almacenGuardado = usuarioRepository.save(userAlmacen);

            // Datos Personales en 3FN para Almacén (Requerido para /api/perfil)
            DatosPersonales datosAlmacen = new DatosPersonales();
            datosAlmacen.setUsuario(almacenGuardado);
            datosAlmacen.setNombres("Carlos");
            datosAlmacen.setApellidos("Operador Almacén");
            datosAlmacen.setDni("71234567");
            datosAlmacen.setSexo(masculino);
            datosAlmacen.setEstadoCivil(soltero);

            DatosPersonales datosAlmacenGuardados = datosPersonalesRepository.save(datosAlmacen);

            // Correo 1:N
            CorreoElectronico correoAlmacen = new CorreoElectronico();
            correoAlmacen.setDatosPersonales(datosAlmacenGuardados);
            correoAlmacen.setCorreoElectronico("almacen@techstore.com");
            correoAlmacen.setTipoCorreo("CORPORATIVO");
            correoRepository.save(correoAlmacen);

            // Teléfono 1:N
            Telefono telefonoAlmacen = new Telefono();
            telefonoAlmacen.setDatosPersonales(datosAlmacenGuardados);
            telefonoAlmacen.setNumeroTelefono("987654322");
            telefonoAlmacen.setTipoTelefono("MOVIL");
            telefonoRepository.save(telefonoAlmacen);

            // Dirección en 3FN
            Direccion dirAlmacen = new Direccion();
            dirAlmacen.setDatosPersonales(datosAlmacenGuardados);
            dirAlmacen.setDistrito(sjl);
            dirAlmacen.setCalleAvenida("Av. Las Flores 124");
            dirAlmacen.setTipoDireccion("DOMICILIO");
            dirAlmacen.setNumero("124");
            dirAlmacen.setReferencia("Almacén Central TechStore");
            direccionRepository.save(dirAlmacen);

            System.out.println(">>> [Hibernate] Usuario 'almacen' y perfil 3FN inicializados exitosamente.");
        } else {
            System.out.println(">>> [Hibernate] La base de datos ya cuenta con el usuario 'almacen'.");
        }
    }
}

