package com.utp.tienda.controladores;

import com.utp.tienda.patrones.fachada.AuthFacade;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.LoginRequestDTO;
import com.utp.tienda.vistas.dto.LoginResponseDTO;
import com.utp.tienda.vistas.dto.RegistroRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador MVC para el Módulo de Acceso y Registro (Auth)
 * Arquitectura: MVC
 * Patrones aplicados: Facade, Proxy, Singleton, Factory Method, Strategy
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;
    private final SesionService sesionService;

    public AuthController(AuthFacade authFacade, SesionService sesionService) {
        this.authFacade = authFacade;
        this.sesionService = sesionService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        sesionService.revocar(authorization);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO respuesta = authFacade.iniciarSesion(request.getNombreUsuario(), request.getContrasena());

        if (respuesta.isExitoso()) {
            return ResponseEntity.ok(respuesta);
        } else if (respuesta.isCuentaBloqueada()) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(respuesta); // HTTP 423 Locked
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta); // HTTP 401 Unauthorized
        }
    }

    @PostMapping({"/registro", "/register"})
    public ResponseEntity<LoginResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        try {
            LoginResponseDTO respuesta = authFacade.registrarCliente(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Datos inválidos para el registro";
            return ResponseEntity.badRequest().body(LoginResponseDTO.fallo(mensaje, 0, false));
        } catch (Exception e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponseDTO.fallo("Error al procesar el registro: " + mensaje, 0, false));
        }

    }
}
