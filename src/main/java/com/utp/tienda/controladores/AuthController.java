package com.utp.tienda.controladores;

import com.utp.tienda.patrones.fachada.AuthFacade;
import com.utp.tienda.modelos.servicios.ControlTraficoService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.modelos.servicios.TurnstileCaptchaService;
import com.utp.tienda.modelos.servicios.VerificacionCorreoService;
import com.utp.tienda.vistas.dto.LoginRequestDTO;
import com.utp.tienda.vistas.dto.LoginResponseDTO;
import com.utp.tienda.vistas.dto.RegistroRequestDTO;
import com.utp.tienda.vistas.dto.ReenviarCodigoDTO;
import com.utp.tienda.vistas.dto.RestablecerPasswordDTO;
import com.utp.tienda.vistas.dto.SolicitarRecuperacionDTO;
import com.utp.tienda.vistas.dto.VerificarCodigoDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador MVC para el Módulo de Acceso, Registro, Verificación 2FA y Recuperación (Auth)
 * Arquitectura: MVC
 * Patrones aplicados: Facade, Proxy, Singleton, Factory Method, Strategy, Adapter, Template Method
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;
    private final SesionService sesionService;
    private final TurnstileCaptchaService turnstileCaptchaService;
    private final ControlTraficoService controlTraficoService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(AuthFacade authFacade,
                          SesionService sesionService,
                          TurnstileCaptchaService turnstileCaptchaService,
                          ControlTraficoService controlTraficoService) {
        this.authFacade = authFacade;
        this.sesionService = sesionService;
        this.turnstileCaptchaService = turnstileCaptchaService;
        this.controlTraficoService = controlTraficoService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sesionService.revocar(authorization);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO respuesta = authFacade.iniciarSesion(request.getNombreUsuario(), request.getContrasena());

            if (respuesta.isExitoso() || respuesta.isRequiere2fa() || respuesta.isRequiereVerificacionCorreo()) {
                return ResponseEntity.ok(respuesta);
            } else if (respuesta.isCuentaBloqueada()) {
                return ResponseEntity.status(HttpStatus.LOCKED).body(respuesta); // HTTP 423 Locked
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta); // HTTP 401 Unauthorized
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LoginResponseDTO.fallo(e.getMessage(), 0, false));
        }
    }

    @PostMapping({"/registro", "/register"})
    public ResponseEntity<LoginResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO request,
                                                      HttpServletRequest servletRequest) {
        String ip = controlTraficoService != null ? controlTraficoService.obtenerIpCliente(servletRequest) : null;
        if (turnstileCaptchaService != null && !turnstileCaptchaService.validar(request.getCaptchaToken(), ip)) {
            return ResponseEntity.badRequest().body(
                    LoginResponseDTO.fallo("Verificación de seguridad (CAPTCHA) no válida. Por favor, inténtalo nuevamente.", 0, false)
            );
        }
        try {
            LoginResponseDTO respuesta = authFacade.registrarCliente(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Datos inválidos para el registro";
            return ResponseEntity.badRequest().body(LoginResponseDTO.fallo(mensaje, 0, false));
        } catch (IllegalStateException e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Servicio de correo no disponible";
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(LoginResponseDTO.fallo(mensaje, 0, false));
        } catch (Exception e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponseDTO.fallo("Error al procesar el registro: " + mensaje, 0, false));
        }
    }

    @PostMapping("/verificar-correo")
    public ResponseEntity<LoginResponseDTO> verificarCorreo(@Valid @RequestBody VerificarCodigoDTO request) {
        return procesarVerificacionOtp(() -> authFacade.verificarCorreoRegistro(request.desafioToken(), request.codigo()));
    }

    @PostMapping("/verificar-2fa")
    public ResponseEntity<LoginResponseDTO> verificarTwoFactor(@Valid @RequestBody VerificarCodigoDTO request) {
        return procesarVerificacionOtp(() -> authFacade.verificarTwoFactor(request.desafioToken(), request.codigo()));
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<?> reenviarCodigo(@Valid @RequestBody ReenviarCodigoDTO request) {
        try {
            VerificacionCorreoService.ResultadoDesafio desafio = authFacade.reenviarCodigo(request.desafioToken());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Se envió un nuevo código de 6 dígitos a tu correo.",
                    "desafioToken", desafio.desafioToken(),
                    "correoDestino", desafio.correoEnmascarado(),
                    "cooldownSegundos", VerificacionCorreoService.COOLDOWN_REENVIO_SEGUNDOS
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<?> recuperarPassword(@Valid @RequestBody SolicitarRecuperacionDTO request,
                                               HttpServletRequest servletRequest) {
        String ip = controlTraficoService != null ? controlTraficoService.obtenerIpCliente(servletRequest) : null;
        if (turnstileCaptchaService != null && !turnstileCaptchaService.validar(request.captchaToken(), ip)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Verificación de seguridad (CAPTCHA) no válida. Por favor, inténtalo nuevamente."
            ));
        }
        try {
            authFacade.solicitarRecuperacionPassword(request.correo());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Si el correo electrónico está registrado en TechStore, recibirás un código de 6 dígitos en los próximos segundos."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<?> restablecerPassword(@Valid @RequestBody RestablecerPasswordDTO request) {
        try {
            authFacade.restablecerPassword(request.correo(), request.codigo(), request.nuevaContrasena());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Tu contraseña se actualizó correctamente. Ya puedes iniciar sesión."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<LoginResponseDTO> procesarVerificacionOtp(java.util.function.Supplier<LoginResponseDTO> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(LoginResponseDTO.fallo(e.getMessage(), 0, false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LoginResponseDTO.fallo(e.getMessage(), 0, false));
        }
    }
}
