package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** DTO para actualizar (parcial) un producto. */
public record ActualizarProductoDTO(
        @Size(max = 200) String nombre,
        @Size(max = 50) String categoria,
        @Positive BigDecimal precio,
        @Min(0) Integer stock,
        @Size(max = 300) String imagen,
        @Size(max = 500) String descripcion,
        @Min(0) Integer vendidos,
        Boolean activo
) {}
