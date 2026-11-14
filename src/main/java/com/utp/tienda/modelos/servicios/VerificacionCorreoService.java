package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.CodigoVerificacion;
import com.utp.tienda.modelos.entidades.CorreoElectronico;
import com.utp.tienda.modelos.entidades.DatosPersonales;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.CodigoVerificacionRepository;
import com.utp.tienda.modelos.repositorios.CorreoElectronicoRepository;
import com.utp.tienda.modelos.repositorios.DatosPersonalesRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.patrones.adapter.IServicioCorreo;
import com.utp.tienda.patrones.template.PlantillaCorreoBase;
import com.utp.tienda.patrones.template.PlantillaCorreoBase.ContextoCorreo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio central de códigos OTP de verificación de correo, 2FA y recuperación de contraseña.
 * Coordina la persistencia 3FN con el Adaptador Brevo y las Plantillas Template Method.
 */
@Service
public class VerificacionCorreoService {

    public static final String PROPOSITO_REGISTRO = "REGISTRO";
    public static final String PROPOSITO_LOGIN_2FA = "LOGIN_2FA";
    public static final String PROPOSITO_RECUPERAR_PASSWORD = "RECUPERAR_PASSWORD";
    public static final String PROPOSITO_BIENVENIDA_ADMIN = "BIENVENIDA_ADMIN";

    public static final int COOLDOWN_REENVIO_SEGUNDOS = 45;
    private static final int MAX_INTENTOS_OTP = 5;
    private static final int TTL_MINUTOS_2FA = 10;
    private static final int TTL_MINUTOS_ESTANDAR = 15;

    private final SecureRandom secureRandom = new SecureRandom();
    private final CodigoVerificacionRepository codigoRepository;
    private final DatosPersonalesRepository datosRepo;
    private final CorreoElectronicoRepository correoRepository;
    private final UsuarioRepository usuarioRepository;
    private final IServicioCorreo servicioCorreo;
    private final PasswordEncryptionStrategy encryptionStrategy;
    private final SesionService sesionService;

    public record ResultadoDesafio(String desafioToken, String correoEnmascarado) {}

    public VerificacionCorreoService(
            CodigoVerificacionRepository codigoRepository,
            DatosPersonalesRepository datosRepo,
            CorreoElectronicoRepository correoRepository,
            UsuarioRepository usuarioRepository,
            IServicioCorreo servicioCorreo,
            @Qualifier("sha256Strategy") PasswordEncryptionStrategy encryptionStrategy,
            SesionService sesionService) {
        this.codigoRepository = codigoRepository;
        this.datosRepo = datosRepo;
        this.correoRepository = correoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioCorreo = servicioCorreo;
        this.encryptionStrategy = encryptionStrategy;
        this.sesionService = sesionService;
    }

    public boolean servicioDisponible() {
        return servicioCorreo.estaConfigurado();
    }

    @Transactional
    public ResultadoDesafio emitirDesafio(Usuario usuario, String proposito) {
        validarServicioDisponible();
        CorreoElectronico correoEntity = obtenerCorreoPrincipal(usuario)
                .orElseThrow(() -> new IllegalStateException(
                        "La cuenta no tiene un correo electrónico asociado para verificación."));

        String emailDestino = correoEntity.getCorreoElectronico().trim();
        String nombreDestinatario = resolverNombreDestinatario(usuario, correoEntity.getDatosPersonales());

        codigoRepository.invalidarActivosPorUsuarioYProposito(usuario.getIdUsuario(), proposito);
        return crearYEnviarDesafio(usuario, emailDestino, nombreDestinatario, proposito);
    }

    @Transactional
    public void emitirRecuperacionPorCorreo(String correo) {
        validarServicioDisponible();
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Ingresa el correo electrónico de tu cuenta.");
        }

        String correoNormalizado = correo.trim().toLowerCase();
        Optional<CorreoElectronico> correoOpt = correoRepository.findByCorreoElectronico(correoNormalizado);
        if (correoOpt.isEmpty() || correoOpt.get().getDatosPersonales() == null
                || correoOpt.get().getDatosPersonales().getUsuario() == null) {
            return; // Respuesta neutra para evitar enumeración de usuarios
        }

        CorreoElectronico correoEntity = correoOpt.get();
        Usuario usuario = correoEntity.getDatosPersonales().getUsuario();
        if ("Inactivo".equalsIgnoreCase(usuario.getEstado())) {
            return;
        }

        Optional<CodigoVerificacion> activo = codigoRepository
                .findTopByUsuarioIdUsuarioAndPropositoAndUsadoFalseAndEstadoTrueOrderByFechaCreacionDesc(
                        usuario.getIdUsuario(), PROPOSITO_RECUPERAR_PASSWORD);

        if (activo.isPresent()) {
            long segundosDesdeUltimo = Duration.between(
                    activo.get().getFechaUltimoReenvio(), LocalDateTime.now()).getSeconds();
            if (segundosDesdeUltimo < COOLDOWN_REENVIO_SEGUNDOS) {
                return; // Protege contra envíos repetidos en pocos segundos
            }
        }

        codigoRepository.invalidarActivosPorUsuarioYProposito(
                usuario.getIdUsuario(), PROPOSITO_RECUPERAR_PASSWORD);
        String nombreDestinatario = resolverNombreDestinatario(usuario, correoEntity.getDatosPersonales());
        crearYEnviarDesafio(usuario, correoEntity.getCorreoElectronico().trim(),
                nombreDestinatario, PROPOSITO_RECUPERAR_PASSWORD);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Usuario verificarCodigoPorDesafio(String desafioToken, String propositoEsperado, String codigoIngresado) {
        if (desafioToken == null || desafioToken.isBlank()) {
            throw new IllegalArgumentException("La sesión de verificación no es válida. Inicia el proceso nuevamente.");
        }

        CodigoVerificacion challenge = codigoRepository.findByDesafioTokenAndEstadoTrue(desafioToken.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El desafío de verificación expiró o no existe. Solicita un nuevo código."));

        if (!propositoEsperado.equalsIgnoreCase(challenge.getProposito())) {
            throw new IllegalArgumentException("El código no corresponde a esta operación.");
        }

        verificarYConsumirChallenge(challenge, codigoIngresado);
        Usuario usuario = challenge.getUsuario();
        if (PROPOSITO_REGISTRO.equalsIgnoreCase(propositoEsperado)) {
            usuario.setCorreoVerificado(true);
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuarioRepository.save(usuario);
        }
        return usuario;
    }

    @Transactional
    public ResultadoDesafio reenviarPorDesafio(String desafioToken) {
        validarServicioDisponible();
        if (desafioToken == null || desafioToken.isBlank()) {
            throw new IllegalArgumentException("Token de verificación inválido.");
        }

        CodigoVerificacion challenge = codigoRepository.findByDesafioTokenAndEstadoTrue(desafioToken.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La solicitud de verificación ya no está vigente. Inicia el proceso nuevamente."));

        if (challenge.getUsado()) {
            throw new IllegalArgumentException("Este código ya fue utilizado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        long transcurridos = Duration.between(challenge.getFechaUltimoReenvio(), ahora).getSeconds();
        if (transcurridos < COOLDOWN_REENVIO_SEGUNDOS) {
            long restantes = COOLDOWN_REENVIO_SEGUNDOS - transcurridos;
            throw new IllegalStateException(
                    "Espera " + restantes + " segundos antes de reenviar otro código.");
        }

        String nuevoOtp = generarOtp();
        int ttl = ttlPorProposito(challenge.getProposito());
        challenge.setCodigoHash(calcularHashOtp(challenge.getDesafioToken(), nuevoOtp));
        challenge.setIntentosFallidos(0);
        challenge.setFechaExpiracion(ahora.plusMinutes(ttl));
        challenge.setFechaUltimoReenvio(ahora);
        codigoRepository.save(challenge);

        Usuario usuario = challenge.getUsuario();
        DatosPersonales datos = datosRepo.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElse(null);
        String nombre = resolverNombreDestinatario(usuario, datos);
        enviarCorreoPlantilla(challenge.getProposito(), challenge.getCorreoDestino(), nombre, nuevoOtp, ttl, null, null);

        return new ResultadoDesafio(challenge.getDesafioToken(), enmascararCorreo(challenge.getCorreoDestino()));
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void restablecerPasswordConCodigo(String correo, String codigo, String nuevaContrasena) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Ingresa el correo electrónico de tu cuenta.");
        }
        String claveNueva = nuevaContrasena != null ? nuevaContrasena.trim() : "";
        if (claveNueva.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        CorreoElectronico correoEntity = correoRepository.findByCorreoElectronico(correo.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o expirado."));

        Usuario usuario = correoEntity.getDatosPersonales() != null
                ? correoEntity.getDatosPersonales().getUsuario()
                : null;
        if (usuario == null) {
            throw new IllegalArgumentException("Código inválido o expirado.");
        }

        CodigoVerificacion challenge = codigoRepository
                .findTopByUsuarioIdUsuarioAndPropositoAndUsadoFalseAndEstadoTrueOrderByFechaCreacionDesc(
                        usuario.getIdUsuario(), PROPOSITO_RECUPERAR_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o expirado. Solicita uno nuevo."));

        verificarYConsumirChallenge(challenge, codigo);

        usuario.setContrasena(encryptionStrategy.encriptar(claveNueva));
        usuario.setIntentosFallidos(0);
        usuario.setNivelBloqueo(0);
        usuario.setBloqueado(false);
        usuario.setBloqueadoHasta(null);
        usuario.setRequiereCambioPassword(false);
        usuario.setCorreoVerificado(true);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        sesionService.revocarPorUsuario(usuario.getIdUsuario());
    }

    public void enviarBienvenidaAdminSiDisponible(Usuario usuario, String correoDestino,
                                                 String nombreCompleto, String passwordTemporal) {
        if (!servicioDisponible() || correoDestino == null || correoDestino.isBlank()) {
            return;
        }
        try {
            enviarCorreoPlantilla(
                    PROPOSITO_BIENVENIDA_ADMIN,
                    correoDestino.trim(),
                    nombreCompleto,
                    null,
                    null,
                    usuario.getNombreUsuario(),
                    passwordTemporal
            );
        } catch (Exception ex) {
            System.err.println("[Brevo] No se pudo enviar correo de bienvenida admin: " + ex.getMessage());
        }
    }

    public boolean tieneCorreoRegistrado(Integer idUsuario) {
        Optional<DatosPersonales> datos = datosRepo.findByUsuario_IdUsuario(idUsuario);
        if (datos.isEmpty()) return false;
        return correoRepository.findByDatosPersonales_IdDatosPersonales(datos.get().getIdDatosPersonales())
                .stream()
                .anyMatch(c -> c.getCorreoElectronico() != null && !c.getCorreoElectronico().isBlank());
    }

    public String enmascararCorreo(String correo) {
        if (correo == null || correo.isBlank()) return "";
        int arroba = correo.indexOf('@');
        if (arroba <= 1) return correo;
        String local = correo.substring(0, arroba);
        String dominio = correo.substring(arroba);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + dominio;
    }

    private void validarServicioDisponible() {
        if (!servicioDisponible()) {
            throw new IllegalStateException(
                    "El envío de correos no está configurado en el servidor (faltan credenciales Brevo).");
        }
    }

    private ResultadoDesafio crearYEnviarDesafio(Usuario usuario, String emailDestino,
                                                 String nombreDestinatario, String proposito) {
        LocalDateTime ahora = LocalDateTime.now();
        int ttl = ttlPorProposito(proposito);
        String codigo = generarOtp();
        String desafioToken = (PROPOSITO_RECUPERAR_PASSWORD.equals(proposito) ? "REC-" : "CH-")
                + UUID.randomUUID();

        CodigoVerificacion entidad = new CodigoVerificacion();
        entidad.setUsuario(usuario);
        entidad.setProposito(proposito);
        entidad.setCorreoDestino(emailDestino);
        entidad.setDesafioToken(desafioToken);
        entidad.setCodigoHash(calcularHashOtp(desafioToken, codigo));
        entidad.setIntentosFallidos(0);
        entidad.setMaxIntentos(MAX_INTENTOS_OTP);
        entidad.setFechaExpiracion(ahora.plusMinutes(ttl));
        entidad.setFechaUltimoReenvio(ahora);
        entidad.setUsado(false);
        entidad.setEstado(true);
        entidad.setUsuarioRegistro(usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "SISTEMA");
        codigoRepository.save(entidad);

        enviarCorreoPlantilla(proposito, emailDestino, nombreDestinatario, codigo, ttl, null, null);
        return new ResultadoDesafio(desafioToken, enmascararCorreo(emailDestino));
    }

    private void verificarYConsumirChallenge(CodigoVerificacion challenge, String codigoIngresado) {
        if (challenge.getUsado() || !challenge.getEstado()) {
            throw new IllegalArgumentException("Este código ya no es válido. Solicita uno nuevo.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isAfter(challenge.getFechaExpiracion())) {
            challenge.setEstado(false);
            codigoRepository.save(challenge);
            throw new IllegalArgumentException("El código ha expirado. Solicita uno nuevo.");
        }

        String codigoLimpio = codigoIngresado != null ? codigoIngresado.trim() : "";
        if (!codigoLimpio.matches("\\d{6}")) {
            throw new IllegalArgumentException("El código debe tener exactamente 6 dígitos.");
        }

        String hashIngresado = calcularHashOtp(challenge.getDesafioToken(), codigoLimpio);
        boolean coincide = MessageDigest.isEqual(
                hashIngresado.getBytes(StandardCharsets.UTF_8),
                challenge.getCodigoHash().getBytes(StandardCharsets.UTF_8)
        );

        if (!coincide) {
            int intentos = challenge.getIntentosFallidos() + 1;
            challenge.setIntentosFallidos(intentos);
            if (intentos >= challenge.getMaxIntentos()) {
                challenge.setEstado(false);
                codigoRepository.save(challenge);
                throw new IllegalArgumentException(
                        "Demasiados intentos incorrectos. Solicita un nuevo código.");
            }
            codigoRepository.save(challenge);
            int restantes = challenge.getMaxIntentos() - intentos;
            throw new IllegalArgumentException(
                    "Código incorrecto. Te quedan " + restantes + " intento" + (restantes == 1 ? "" : "s") + ".");
        }

        challenge.setUsado(true);
        challenge.setEstado(false);
        challenge.setFechaUso(ahora);
        codigoRepository.save(challenge);
    }

    private void enviarCorreoPlantilla(String proposito, String emailDestino, String nombreDestinatario,
                                       String codigoOtp, Integer ttlMinutos,
                                       String nombreUsuario, String passwordTemporal) {
        PlantillaCorreoBase plantilla = PlantillaCorreoBase.paraProposito(proposito);
        ContextoCorreo ctx = new ContextoCorreo(
                nombreDestinatario,
                codigoOtp,
                ttlMinutos,
                nombreUsuario,
                passwordTemporal
        );
        servicioCorreo.enviarCorreo(
                emailDestino,
                nombreDestinatario,
                plantilla.construirAsunto(ctx),
                plantilla.construirHtml(ctx)
        );
    }

    private Optional<CorreoElectronico> obtenerCorreoPrincipal(Usuario usuario) {
        Optional<DatosPersonales> datos = datosRepo.findByUsuario_IdUsuario(usuario.getIdUsuario());
        if (datos.isEmpty()) {
            return Optional.empty();
        }
        List<CorreoElectronico> lista = correoRepository
                .findByDatosPersonales_IdDatosPersonales(datos.get().getIdDatosPersonales());
        return lista.stream()
                .filter(c -> c.getCorreoElectronico() != null && !c.getCorreoElectronico().isBlank())
                .findFirst();
    }

    private String resolverNombreDestinatario(Usuario usuario, DatosPersonales datos) {
        if (datos != null) {
            String nombres = datos.getNombres() != null ? datos.getNombres().trim() : "";
            String apellidos = datos.getApellidos() != null ? datos.getApellidos().trim() : "";
            String completo = (nombres + " " + apellidos).trim();
            if (!completo.isBlank()) return completo;
        }
        return usuario != null && usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "cliente";
    }

    private int ttlPorProposito(String proposito) {
        return PROPOSITO_LOGIN_2FA.equalsIgnoreCase(proposito) ? TTL_MINUTOS_2FA : TTL_MINUTOS_ESTANDAR;
    }

    private String generarOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String calcularHashOtp(String desafioToken, String codigo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((desafioToken + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el hash de verificación", ex);
        }
    }
}

