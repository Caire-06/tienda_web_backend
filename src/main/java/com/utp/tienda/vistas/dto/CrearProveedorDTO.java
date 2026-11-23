package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO para crear un proveedor (entrada de mercadería). */
public record CrearProveedorDTO(
        @NotBlank String nombre,
        String ruc,
        String contacto,
        String telefono,
        String email,
        String direccion,
        Boolean activo
) {}
