package com.utp.tienda.vistas.dto;

public record UsuarioDTO(
        Integer id,
        String username,
        String nombreCompleto,
        String correo,
        String telefono,
        String rol,
        Boolean bloqueado,
        String estado,
        String fechaRegistro,
        String ultimoAcceso
) {}
