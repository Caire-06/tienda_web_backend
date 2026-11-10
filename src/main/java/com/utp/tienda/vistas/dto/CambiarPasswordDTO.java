package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordDTO(
        @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres") String passwordNueva
) {}
