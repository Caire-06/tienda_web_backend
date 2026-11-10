package com.utp.tienda.vistas.dto;

public record ConsultaDniDTO(
        String dni,
        String nombreCompleto,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String fuente
) {}
