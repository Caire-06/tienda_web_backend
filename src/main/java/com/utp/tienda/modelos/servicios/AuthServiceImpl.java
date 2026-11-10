package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.CorreoElectronico;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.CorreoElectronicoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.LoginRequestDTO;
import com.utp.tienda.vistas.dto.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;

@Service("realAuthService")
public class AuthServiceImpl implements IAuthService {

    private final SesionService sesionService;
    private final UsuarioRepository usuarioRepository;
    private final CorreoElectronicoRepository correoRepository;
    private final PasswordEncryptionStrategy encryptionStrategy;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           CorreoElectronicoRepository correoRepository,
                           SesionService sesionService,
                           @Qualifier("sha256Strategy") PasswordEncryptionStrategy encryptionStrategy) {
        this.sesionService = sesionService;
        this.usuarioRepository = usuarioRepository;
        this.correoRepository = correoRepository;
        this.encryptionStrategy = encryptionStrategy;
    }

    @Override
    @Transactional
    public LoginResponseDTO procesarLogin(LoginRequestDTO request) {
        if (request == null || request.getNombreUsuario() == null) {
            return LoginResponseDTO.fallo("Credenciales inválidas", 0, false);
        }

        String acceso = request.getNombreUsuario().trim();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNombreUsuario(acceso);

        // Si no se encuentra directamente por nombre de usuario, buscar por correo electrónico registrado
        if (usuarioOpt.isEmpty()) {
            Optional<CorreoElectronico> correoOpt = correoRepository.findByCorreoElectronico(acceso);
            if (correoOpt.isPresent()) {
                CorreoElectronico ce = correoOpt.get();
                if (ce.getDatosPersonales() != null && ce.getDatosPersonales().getUsuario() != null) {
                    usuarioOpt = Optional.of(ce.getDatosPersonales().getUsuario());
                }
            }
        }

        if (usuarioOpt.isEmpty()) {
            return LoginResponseDTO.fallo("Credenciales inválidas", 0, false);
        }

        Usuario usuario = usuarioOpt.get();

        // Migración sin SQL manual: los bloqueos permanentes anteriores se liberan
        // al primer intento porque no tenían fecha de expiración.
        LocalDateTime ahora = LocalDateTime.now();
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            LocalDateTime hasta = usuario.getBloqueadoHasta();
            if (hasta != null && ahora.isBefore(hasta)) {
                int segundos = (int) Math.max(1, (Duration.between(ahora, hasta).toMillis() + 999) / 1000);
                return LoginResponseDTO.bloqueoTemporal(segundos);
            }
            usuario.setBloqueado(false);
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
        }

        // 2. Verificar contraseña con la estrategia configurada
        boolean claveCorrecta = encryptionStrategy.verificar(request.getContrasena(), usuario.getContrasena());

        if (claveCorrecta) {
            // Éxito: Reiniciar contador de intentos y actualizar último acceso
            usuario.setIntentosFallidos(0);
            usuario.setNivelBloqueo(0);
            usuario.setBloqueado(false);
            usuario.setBloqueadoHasta(null);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            String token = sesionService.crear(usuario.getIdUsuario());
            String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombreRol() : "CLIENTE";
            return LoginResponseDTO.exito(
                    "Inicio de sesión exitoso",
                    token,
                    rolNombre,
                    usuario.getNombreUsuario(),
                    Boolean.TRUE.equals(usuario.getRequiereCambioPassword())
            );
        } else {
            // Contraseña errónea: Incrementar contador
            int nuevosIntentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
            usuario.setIntentosFallidos(nuevosIntentos);

            if (nuevosIntentos >= 3) {
                // 3 errores por ronda: 10s, 20s, 30s, 60s (máximo).
                int nivel = usuario.getNivelBloqueo();
                int[] tiempos = {10, 20, 30, 60};
                int segundos = tiempos[Math.min(nivel, tiempos.length - 1)];
                usuario.setNivelBloqueo(nivel + 1);
                usuario.setBloqueado(true);
                usuario.setBloqueadoHasta(LocalDateTime.now().plusSeconds(segundos));
                usuario.setIntentosFallidos(0);
                usuarioRepository.save(usuario);
                return LoginResponseDTO.bloqueoTemporal(segundos);
            } else {
                usuarioRepository.save(usuario);
                int restantes = 3 - nuevosIntentos;
                return LoginResponseDTO.fallo("Contraseña incorrecta. Le quedan " + restantes + " intento(s).", restantes, false);
            }
        }
    }
}
