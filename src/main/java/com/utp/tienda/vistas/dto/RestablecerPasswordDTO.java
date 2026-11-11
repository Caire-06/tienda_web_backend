package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordDTO(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Ingresa un correo electrónico válido")
        String correo,
        @NotBlank(message = "El código de verificación es obligatorio")
        String codigo,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String nuevaContrasena
) {}

