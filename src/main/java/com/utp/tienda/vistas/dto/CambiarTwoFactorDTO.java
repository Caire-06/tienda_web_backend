package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarTwoFactorDTO(
        @NotNull(message = "El estado de verificación en dos pasos es obligatorio")
        Boolean activo
) {}

