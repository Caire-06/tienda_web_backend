package com.utp.tienda.patrones.fachada;

import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.servicios.IAuthService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.modelos.servicios.UsuarioService;
import com.utp.tienda.modelos.servicios.VerificacionCorreoService;
import com.utp.tienda.vistas.dto.LoginRequestDTO;
import com.utp.tienda.vistas.dto.LoginResponseDTO;
import com.utp.tienda.vistas.dto.RegistroRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patrón Estructural: Facade (Fachada)
 * Proporciona una interfaz unificada y de alto nivel que oculta la complejidad
 * de los subsistemas de autenticación, verificación por correo, 2FA y seguridad.
 */
@Component
public class AuthFacade {

    private final IAuthService authService;
    private final UsuarioService usuarioService;
    private final SesionService sesionService;
    private final VerificacionCorreoService verificacionCorreoService;

    public AuthFacade(@Qualifier("authProxy") IAuthService authService,
                      UsuarioService usuarioService,
                      SesionService sesionService,
                      VerificacionCorreoService verificacionCorreoService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
        this.verificacionCorreoService = verificacionCorreoService;
    }

    public LoginResponseDTO iniciarSesion(String nombreUsuario, String contrasena) {
        LoginRequestDTO dto = new LoginRequestDTO(nombreUsuario, contrasena);
        return authService.procesarLogin(dto);
    }

    @Transactional
    public LoginResponseDTO registrarCliente(RegistroRequestDTO dto) {
        Usuario nuevo = usuarioService.registrarCliente(dto);
        VerificacionCorreoService.ResultadoDesafio desafio =
                verificacionCorreoService.emitirDesafio(nuevo, VerificacionCorreoService.PROPOSITO_REGISTRO);
        return LoginResponseDTO.verificacionCorreoRequerida(
                desafio.desafioToken(),
                desafio.correoEnmascarado(),
                "Registro creado. Te enviamos un código de 6 dígitos por correo para activar tu cuenta."
        );
    }

    public LoginResponseDTO verificarCorreoRegistro(String desafioToken, String codigo) {
        Usuario usuario = verificacionCorreoService.verificarCodigoPorDesafio(
                desafioToken, VerificacionCorreoService.PROPOSITO_REGISTRO, codigo);
        return emitirSesionCompleta(usuario, "Correo verificado con éxito. ¡Bienvenido a TechStore!");
    }

    public LoginResponseDTO verificarTwoFactor(String desafioToken, String codigo) {
        Usuario usuario = verificacionCorreoService.verificarCodigoPorDesafio(
                desafioToken, VerificacionCorreoService.PROPOSITO_LOGIN_2FA, codigo);
        return emitirSesionCompleta(usuario, "Verificación en dos pasos completada");
    }

    public VerificacionCorreoService.ResultadoDesafio reenviarCodigo(String desafioToken) {
        return verificacionCorreoService.reenviarPorDesafio(desafioToken);
    }

    public void solicitarRecuperacionPassword(String correo) {
        verificacionCorreoService.emitirRecuperacionPorCorreo(correo);
    }

    public void restablecerPassword(String correo, String codigo, String nuevaContrasena) {
        verificacionCorreoService.restablecerPasswordConCodigo(correo, codigo, nuevaContrasena);
    }

    private LoginResponseDTO emitirSesionCompleta(Usuario usuario, String mensaje) {
        String token = sesionService.crear(usuario.getIdUsuario());
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombreRol() : "CLIENTE";
        return LoginResponseDTO.exito(
                mensaje,
                token,
                rolNombre,
                usuario.getNombreUsuario(),
                Boolean.TRUE.equals(usuario.getRequiereCambioPassword())
        );
    }
}
