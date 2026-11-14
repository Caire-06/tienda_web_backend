package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificarCodigoDTO(
        @NotBlank(message = "El token de verificación es obligatorio")
        String desafioToken,
        @NotBlank(message = "El código de 6 dígitos es obligatorio")
        String codigo
) {}

