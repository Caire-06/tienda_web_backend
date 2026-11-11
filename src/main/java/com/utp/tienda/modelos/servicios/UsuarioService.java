package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.factorias.UsuarioFactory;
import com.utp.tienda.modelos.repositorios.*;
import com.utp.tienda.patrones.memento.UsuarioCaretaker;
import com.utp.tienda.patrones.memento.UsuarioMemento;
import com.utp.tienda.vistas.dto.RegistroRequestDTO;
import com.utp.tienda.vistas.dto.UsuarioDTO;
import com.utp.tienda.vistas.dto.CrearUsuarioAdminDTO;
import com.utp.tienda.vistas.dto.UsuarioCreadoAdminDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DatosPersonalesRepository datosPersonalesRepository;
    private final CorreoElectronicoRepository correoRepository;
    private final DireccionRepository direccionRepository;
    private final TelefonoRepository telefonoRepository;
    private final DistritoRepository distritoRepository;
    private final SexoRepository sexoRepository;
    private final EstadoCivilRepository estadoCivilRepository;
    private final UsuarioFactory usuarioFactory;
    private final UsuarioCaretaker caretaker;
    private final PasswordEncryptionStrategy encryptionStrategy;
    private final VerificacionCorreoService verificacionCorreoService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          DatosPersonalesRepository datosPersonalesRepository,
                          CorreoElectronicoRepository correoRepository,
                          DireccionRepository direccionRepository,
                          TelefonoRepository telefonoRepository,
                          DistritoRepository distritoRepository,
                          SexoRepository sexoRepository,
                          EstadoCivilRepository estadoCivilRepository,
                          UsuarioFactory usuarioFactory,
                          UsuarioCaretaker caretaker,
                          @Qualifier("sha256Strategy") PasswordEncryptionStrategy encryptionStrategy,
                          VerificacionCorreoService verificacionCorreoService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.datosPersonalesRepository = datosPersonalesRepository;
        this.correoRepository = correoRepository;
        this.direccionRepository = direccionRepository;
        this.telefonoRepository = telefonoRepository;
        this.distritoRepository = distritoRepository;
        this.sexoRepository = sexoRepository;
        this.estadoCivilRepository = estadoCivilRepository;
        this.usuarioFactory = usuarioFactory;
        this.caretaker = caretaker;
        this.encryptionStrategy = encryptionStrategy;
        this.verificacionCorreoService = verificacionCorreoService;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByNombreUsuario(username);
    }

    @Transactional
    public Usuario registrarUsuario(String username, String rawPassword, String nombreRol) {
        if (usuarioRepository.existsByNombreUsuario(username)) {
            throw new IllegalArgumentException("El usuario ya existe");
        }
        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseGet(() -> rolRepository.save(new Rol(null, nombreRol, "Rol creado dinámicamente")));

        String hash = encryptionStrategy.encriptar(rawPassword);
        // Patrón Factory Method
        Usuario nuevo = usuarioFactory.crearUsuario(username, hash, rol);
        return usuarioRepository.save(nuevo);
    }

    @Transactional
    public Usuario registrarCliente(RegistroRequestDTO dto) {
        String correoLimpio = dto.getCorreo().trim().toLowerCase();

        // 1. Validar unicidad de correo y nombre de usuario
        if (correoRepository.existsByCorreoElectronico(correoLimpio) || usuarioRepository.existsByNombreUsuario(correoLimpio)) {
            throw new IllegalArgumentException("Ese correo electrónico ya se encuentra registrado.");
        }

        // 2. Rol CLIENTE
        Rol rolCliente = rolRepository.findByNombreRol("CLIENTE")
                .orElseGet(() -> rolRepository.save(new Rol(null, "CLIENTE", "Acceso a catálogo y compras")));

        // 3. Encriptar contraseña usando el patrón Strategy
        String hash = encryptionStrategy.encriptar(dto.getPassword());

        // 4. Instanciar Usuario usando el patrón Factory Method
        Usuario nuevoUsuario = usuarioFactory.crearUsuario(correoLimpio, hash, rolCliente);
        nuevoUsuario.setCorreoVerificado(false);
        nuevoUsuario.setTwoFactorActivo(false);
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // 5. Crear Datos Personales en 3FN
        Sexo sexoDefault = sexoRepository.findByDescripcion("No Especifica")
                .orElseGet(() -> sexoRepository.save(new Sexo(null, "No Especifica")));
        EstadoCivil estadoCivilDefault = estadoCivilRepository.findByDescripcion("Soltero(a)")
                .orElseGet(() -> estadoCivilRepository.save(new EstadoCivil(null, "Soltero(a)")));

        DatosPersonales datos = new DatosPersonales();
        datos.setUsuario(usuarioGuardado);
        datos.setNombres(dto.getNombre().trim());
        datos.setApellidos(dto.getApellido().trim());
        String dni = (dto.getDni() != null && !dto.getDni().isBlank()) 
                ? dto.getDni().trim() 
                : "CLI" + String.format("%08d", usuarioGuardado.getIdUsuario());
        datos.setDni(dni);
        datos.setSexo(sexoDefault);
        datos.setEstadoCivil(estadoCivilDefault);

        DatosPersonales datosGuardados = datosPersonalesRepository.save(datos);

        // 6. Registrar Correo Electrónico en 3FN (relación 1:N)
        CorreoElectronico correo = new CorreoElectronico();
        correo.setDatosPersonales(datosGuardados);
        correo.setCorreoElectronico(correoLimpio);
        correo.setTipoCorreo("PERSONAL");
        correoRepository.save(correo);

        // 7. Registrar Dirección si fue provista
        if (dto.getDireccion() != null && !dto.getDireccion().trim().isEmpty()) {
            Distrito distrito = distritoRepository.findByNombreDistrito("San Juan de Lurigancho")
                    .orElseGet(() -> distritoRepository.findAll().stream().findFirst().orElse(null));

            Direccion direccion = new Direccion();
            direccion.setDatosPersonales(datosGuardados);
            direccion.setDistrito(distrito);
            direccion.setCalleAvenida(dto.getDireccion().trim());
            direccion.setTipoDireccion("DOMICILIO");
            direccion.setNumero("S/N");
            direccion.setReferencia("Registrado vía Web");
            direccionRepository.save(direccion);
        }

        // 8. Registrar Teléfono si fue provisto
        if (dto.getTelefono() != null && !dto.getTelefono().trim().isEmpty()) {
            Telefono tel = new Telefono();
            tel.setDatosPersonales(datosGuardados);
            tel.setNumeroTelefono(dto.getTelefono().trim());
            tel.setTipoTelefono("MOVIL");
            telefonoRepository.save(tel);
        }

        return usuarioGuardado;
    }

    @Transactional
    public Usuario actualizarUsuario(Integer id, String nuevoUsername, Boolean bloqueado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        // Patrón Memento: Guardar estado anterior antes de modificar
        caretaker.guardarEstado(new UsuarioMemento(usuario.getNombreUsuario(), usuario.getIntentosFallidos(), usuario.getBloqueado()));

        if (nuevoUsername != null) usuario.setNombreUsuario(nuevoUsername);
        if (bloqueado != null) usuario.setBloqueado(bloqueado);
        usuario.setFechaActualizacion(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioDTO darDeBajaLogica(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
        usuario.setEstado("Inactivo");
        usuario.setBloqueado(true);
        usuario.setFechaActualizacion(LocalDateTime.now());
        return mapearUsuarioDTO(usuarioRepository.save(usuario));
    }

    public void eliminarUsuario(Integer id) {
        // En cumplimiento de 3FN y auditoría, baja lógica en lugar de borrado físico
        darDeBajaLogica(id);
    }

    public UsuarioDTO mapearUsuarioDTO(Usuario u) {
        String nombreCompleto = u.getNombreUsuario();
        String correo = "";
        String telefono = "—";

        Optional<DatosPersonales> dpOpt = datosPersonalesRepository.findByUsuario_IdUsuario(u.getIdUsuario());
        if (dpOpt.isPresent()) {
            DatosPersonales dp = dpOpt.get();
            if (dp.getNombres() != null) {
                nombreCompleto = (dp.getNombres() + " " + (dp.getApellidos() != null ? dp.getApellidos() : "")).trim();
            }
            List<CorreoElectronico> correos = correoRepository.findByDatosPersonales_IdDatosPersonales(dp.getIdDatosPersonales());
            if (!correos.isEmpty()) {
                correo = correos.get(0).getCorreoElectronico();
            }
            List<Telefono> tels = telefonoRepository.findByDatosPersonales_IdDatosPersonales(dp.getIdDatosPersonales());
            if (!tels.isEmpty()) {
                telefono = tels.get(0).getNumeroTelefono();
            }
        }
        if (correo.isBlank()) {
            correo = u.getNombreUsuario().contains("@") ? u.getNombreUsuario() : u.getNombreUsuario() + "@techstore.com";
        }

        String rol = u.getRol() != null ? u.getRol().getNombreRol() : "CLIENTE";
        String ultimo = u.getUltimoAcceso() != null ? u.getUltimoAcceso().format(DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")) : "—";
        String estado = u.getEstado();
        String fechaRegistro = u.getFechaCreacion() != null 
                ? u.getFechaCreacion().format(DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")) 
                : "Reciente";

        return new UsuarioDTO(u.getIdUsuario(), u.getNombreUsuario(), nombreCompleto, correo, telefono, rol,
                u.getBloqueado(), estado, fechaRegistro, ultimo, Boolean.TRUE.equals(u.getRequiereCambioPassword()));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuariosDTO() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearUsuarioDTO)
                .toList();
    }

    private Rol resolverRolPermitido(String nombreRol) {
        String rolNormalizado = nombreRol != null ? nombreRol.trim().toUpperCase() : "";
        if (!java.util.Set.of("ADMINISTRADOR", "ALMACEN", "CLIENTE").contains(rolNormalizado)) {
            throw new IllegalArgumentException(
                    "Rol no válido: '" + nombreRol + "'. Roles permitidos: ADMINISTRADOR, ALMACEN, CLIENTE.");
        }
        return rolRepository.findByNombreRol(rolNormalizado)
                .orElseThrow(() -> new IllegalStateException("El rol " + rolNormalizado + " no está registrado en el sistema."));
    }

    @Transactional
    public UsuarioDTO cambiarRol(Integer id, String nombreRol) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
        Rol rol = resolverRolPermitido(nombreRol);
        usuario.setRol(rol);
        usuario.setFechaActualizacion(LocalDateTime.now());
        return mapearUsuarioDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO cambiarEstadoBloqueo(Integer id, Boolean bloqueado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
        caretaker.guardarEstado(new UsuarioMemento(usuario.getNombreUsuario(), usuario.getIntentosFallidos(), usuario.getBloqueado()));
        usuario.setBloqueado(bloqueado);
        if (Boolean.FALSE.equals(bloqueado)) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setNivelBloqueo(0);
        }
        usuario.setFechaActualizacion(LocalDateTime.now());
        return mapearUsuarioDTO(usuarioRepository.save(usuario));
    }

    private static String normalizarSegmentoNombre(String valor, String respaldo) {
        if (valor == null || valor.isBlank()) return respaldo;
        String primerToken = valor.trim().split("\\s+")[0];
        String sinTildes = Normalizer.normalize(primerToken, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z]", "");
        if (sinTildes.isEmpty()) return respaldo;
        return Character.toUpperCase(sinTildes.charAt(0)) + sinTildes.substring(1).toLowerCase();
    }

    /**
     * Genera una contraseña temporal determinística a partir de nombre, apellido y DNI.
     * Formato: primera letra del nombre (mayúscula) + primer apellido (minúscula) + últimos 3 dígitos del DNI
     * (ej. Carlos + Castro + 96969696 -> Ccastro696).
     */
    public static String generarPasswordTemporal(String nombres, String apellidos, String dni) {
        String parteNombre = normalizarSegmentoNombre(nombres, "Usuario");
        String parteApellido = normalizarSegmentoNombre(apellidos, "Tech");
        String digitosDni = dni != null ? dni.replaceAll("\\D", "") : "";
        String ultimos3Dni = digitosDni.length() >= 3
                ? digitosDni.substring(digitosDni.length() - 3)
                : digitosDni;
        String inicial = String.valueOf(Character.toUpperCase(parteNombre.charAt(0)));
        return inicial + parteApellido.toLowerCase() + ultimos3Dni;
    }

    private static String normalizarTipoContacto(String tipo, String respaldo) {
        if (tipo == null || tipo.isBlank()) return respaldo;
        String limpio = Normalizer.normalize(tipo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase();
        return limpio.length() > 30 ? limpio.substring(0, 30) : limpio;
    }

    @Transactional
    public UsuarioCreadoAdminDTO crearUsuarioDesdeAdmin(CrearUsuarioAdminDTO dto) {
        String username = dto.username() != null ? dto.username().trim() : "";
        if (username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (usuarioRepository.existsByNombreUsuario(username)) {
            throw new IllegalArgumentException("El usuario '" + username + "' ya existe.");
        }

        String nombreRol = (dto.rol() != null && !dto.rol().isBlank()) ? dto.rol().trim().toUpperCase() : "ALMACEN";
        if (!java.util.Set.of("ALMACEN", "ADMINISTRADOR").contains(nombreRol)) {
            throw new IllegalArgumentException("Desde administración solo se crean usuarios ALMACEN o ADMINISTRADOR.");
        }
        Rol rol = resolverRolPermitido(nombreRol);

        String nombres = dto.nombres() != null ? dto.nombres().trim() : "";
        String apellidos = dto.apellidos() != null ? dto.apellidos().trim() : "";
        if (nombres.isEmpty() || apellidos.isEmpty()) {
            throw new IllegalArgumentException("Nombres y apellidos son obligatorios.");
        }

        String dniLimpio = dto.dni() != null ? dto.dni().trim() : "";
        if (!dniLimpio.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos numéricos.");
        }
        if (datosPersonalesRepository.findByDni(dniLimpio).isPresent()) {
            throw new IllegalArgumentException("Ya existe una persona registrada con el DNI " + dniLimpio + ".");
        }

        if (dto.correo() != null && !dto.correo().isBlank()) {
            String correoLimpio = dto.correo().trim().toLowerCase();
            if (correoRepository.existsByCorreoElectronico(correoLimpio)) {
                throw new IllegalArgumentException("El correo '" + correoLimpio + "' ya está registrado.");
            }
        }

        String passwordTemporal = generarPasswordTemporal(nombres, apellidos, dniLimpio);
        String hash = encryptionStrategy.encriptar(passwordTemporal);
        Usuario nuevo = usuarioFactory.crearUsuario(username, hash, rol);
        nuevo.setRequiereCambioPassword(true);
        nuevo.setCorreoVerificado(true);
        nuevo.setTwoFactorActivo(false);
        nuevo.setUsuarioRegistro("ADMIN");
        Usuario guardado = usuarioRepository.save(nuevo);

        Sexo sexoDefault = sexoRepository.findByDescripcion("No Especifica")
                .orElseGet(() -> sexoRepository.save(new Sexo(null, "No Especifica")));
        EstadoCivil estadoCivilDefault = estadoCivilRepository.findByDescripcion("Soltero(a)")
                .orElseGet(() -> estadoCivilRepository.save(new EstadoCivil(null, "Soltero(a)")));

        DatosPersonales dp = new DatosPersonales();
        dp.setUsuario(guardado);
        dp.setNombres(nombres);
        dp.setApellidos(apellidos);
        dp.setDni(dniLimpio);
        dp.setSexo(sexoDefault);
        dp.setEstadoCivil(estadoCivilDefault);
        DatosPersonales dpGuardado = datosPersonalesRepository.save(dp);

        if (dto.correo() != null && !dto.correo().isBlank()) {
            CorreoElectronico ce = new CorreoElectronico();
            ce.setDatosPersonales(dpGuardado);
            ce.setCorreoElectronico(dto.correo().trim().toLowerCase());
            ce.setTipoCorreo(normalizarTipoContacto(dto.tipoCorreo(), "CORPORATIVO"));
            correoRepository.save(ce);
        }

        if (dto.telefono() != null && !dto.telefono().isBlank()) {
            Telefono tel = new Telefono();
            tel.setDatosPersonales(dpGuardado);
            tel.setNumeroTelefono(dto.telefono().trim());
            tel.setTipoTelefono(normalizarTipoContacto(dto.tipoTelefono(), "MOVIL"));
            telefonoRepository.save(tel);
        }

        verificacionCorreoService.enviarBienvenidaAdminSiDisponible(
                guardado, dto.correo(), (nombres + " " + apellidos).trim(), passwordTemporal);

        return new UsuarioCreadoAdminDTO(mapearUsuarioDTO(guardado), passwordTemporal);
    }
}
