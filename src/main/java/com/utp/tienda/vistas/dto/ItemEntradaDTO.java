package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Línea de detalle de una entrada de mercadería. */
public record ItemEntradaDTO(
        @NotNull Integer idProducto,
        @NotNull @Min(1) Integer cantidad,
        @NotNull BigDecimal costoUnitario
) {}
