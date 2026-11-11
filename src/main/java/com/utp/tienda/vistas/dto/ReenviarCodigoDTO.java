package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;

public record ReenviarCodigoDTO(
        @NotBlank(message = "El token de verificación es obligatorio")
        String desafioToken
) {}

