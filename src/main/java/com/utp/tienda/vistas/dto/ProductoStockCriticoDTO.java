package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;

public record ProductoStockCriticoDTO(
        Integer id,
        String nombre,
        String categoria,
        Integer stock,
        BigDecimal precio
) {}

