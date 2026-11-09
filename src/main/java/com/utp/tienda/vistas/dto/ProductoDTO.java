package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** DTO de respuesta para productos. */
public record ProductoDTO(
        Integer id,
        String nombre,
        String categoria,
        BigDecimal precio,
        Integer stock,
        Integer vendidos,
        String imagen,
        String descripcion,
        Boolean activo
) {}
