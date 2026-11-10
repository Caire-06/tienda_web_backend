package com.utp.tienda.vistas.dto;

public record UsuarioCreadoAdminDTO(
        UsuarioDTO usuario,
        String passwordTemporal
) {}
