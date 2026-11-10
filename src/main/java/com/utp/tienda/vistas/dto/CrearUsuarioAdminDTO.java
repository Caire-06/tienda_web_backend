package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearUsuarioAdminDTO(
        @NotBlank String username,
        String password,
        String rol,
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotBlank String dni,
        String correo,
        String tipoCorreo,
        String telefono,
        String tipoTelefono
) {}

