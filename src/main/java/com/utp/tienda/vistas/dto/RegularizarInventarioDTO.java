package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Solicitud para regularizar el costo unitario inicial de un producto ya existente. */
public record RegularizarInventarioDTO(
        @NotNull(message = "El producto es obligatorio") Integer productoId,
        @NotNull(message = "El costo unitario inicial es obligatorio")
        @DecimalMin(value = "0", message = "El costo unitario inicial no puede ser negativo")
        BigDecimal costoUnitario
) {}
