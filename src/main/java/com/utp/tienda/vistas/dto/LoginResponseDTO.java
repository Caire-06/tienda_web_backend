package com.utp.tienda.vistas.dto;

public class LoginResponseDTO {

    private boolean exitoso;
    private String mensaje;
    private Integer intentosRestantes;
    private boolean cuentaBloqueada;
    private String token;
    private String rol;
    private String nombreUsuario;
    private Integer segundosEspera;
    private boolean requiereCambioPassword;
    private boolean requiereVerificacionCorreo;
    private boolean requiere2fa;
    private String desafioToken;
    private String correoDestino;

    public LoginResponseDTO() {}

    public static LoginResponseDTO exito(String mensaje, String token, String rol, String nombreUsuario) {
        return exito(mensaje, token, rol, nombreUsuario, false);
    }

    public static LoginResponseDTO exito(String mensaje, String token, String rol, String nombreUsuario,
                                         boolean requiereCambioPassword) {
        LoginResponseDTO r = new LoginResponseDTO();
        r.exitoso = true;
        r.mensaje = mensaje;
        r.token = token;
        r.rol = rol;
        r.nombreUsuario = nombreUsuario;
        r.cuentaBloqueada = false;
        r.intentosRestantes = 3;
        r.segundosEspera = 0;
        r.requiereCambioPassword = requiereCambioPassword;
        return r;
    }

    public static LoginResponseDTO fallo(String mensaje, int intentosRestantes, boolean cuentaBloqueada) {
        LoginResponseDTO r = new LoginResponseDTO();
        r.exitoso = false;
        r.mensaje = mensaje;
        r.intentosRestantes = Math.max(0, intentosRestantes);
        r.cuentaBloqueada = cuentaBloqueada;
        r.segundosEspera = 0;
        return r;
    }

    public static LoginResponseDTO bloqueoTemporal(int segundos) {
        LoginResponseDTO r = fallo("Demasiados intentos. Vuelve a intentarlo en " + segundos + " segundo(s).", 0, true);
        r.segundosEspera = segundos;
        return r;
    }

    public static LoginResponseDTO verificacionCorreoRequerida(String desafioToken, String correoDestino, String mensaje) {
        LoginResponseDTO r = new LoginResponseDTO();
        r.exitoso = false;
        r.mensaje = mensaje;
        r.requiereVerificacionCorreo = true;
        r.desafioToken = desafioToken;
        r.correoDestino = correoDestino;
        r.intentosRestantes = 3;
        r.segundosEspera = 0;
        return r;
    }

    public static LoginResponseDTO twoFactorRequerido(String desafioToken, String correoDestino) {
        LoginResponseDTO r = new LoginResponseDTO();
        r.exitoso = false;
        r.mensaje = "Se envió un código de verificación en dos pasos a tu correo electrónico.";
        r.requiere2fa = true;
        r.desafioToken = desafioToken;
        r.correoDestino = correoDestino;
        r.intentosRestantes = 3;
        r.segundosEspera = 0;
        return r;
    }

    public Integer getSegundosEspera() { return segundosEspera; }

    public boolean isExitoso() { return exitoso; }
    public String getMensaje() { return mensaje; }
    public Integer getIntentosRestantes() { return intentosRestantes; }
    public boolean isCuentaBloqueada() { return cuentaBloqueada; }
    public String getToken() { return token; }
    public String getRol() { return rol; }
    public String getNombreUsuario() { return nombreUsuario; }
    public boolean isRequiereCambioPassword() { return requiereCambioPassword; }
    public boolean isRequiereVerificacionCorreo() { return requiereVerificacionCorreo; }
    public boolean isRequiere2fa() { return requiere2fa; }
    public String getDesafioToken() { return desafioToken; }
    public String getCorreoDestino() { return correoDestino; }
}

