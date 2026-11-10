package com.utp.tienda.vistas.dto;

/** DTO específico para perfil; no expone entidad Usuario ni contraseñas. */
public record PerfilDTO(Integer idUsuario, String nombres, String apellidos, String documento,
                        String correo, String telefono, String direccion, String ciudad,
                        String rol, Boolean requiereCambioPassword) {}
