package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearUsuarioAdminDTO(
        @NotBlank String username,
        @NotBlank String password,
        String rol,
        String nombres,
        String apellidos,
        String correo,
        String telefono
) {}

