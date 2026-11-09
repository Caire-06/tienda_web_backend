package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** DTO para crear un producto (con imagen servida por Cloudinary). */
public record CrearProductoDTO(
        @NotBlank @Size(max = 200) String nombre,
        @Size(max = 50) String categoria,
        @NotNull @Positive BigDecimal precio,
        @NotNull @Min(0) Integer stock,
        @Size(max = 300) String imagen,
        @Size(max = 500) String descripcion,
        @Min(0) Integer vendidos,
        Boolean activo
) {}
