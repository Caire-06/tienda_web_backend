package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRecuperacionDTO(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Ingresa un correo electrónico válido")
        String correo
) {}

