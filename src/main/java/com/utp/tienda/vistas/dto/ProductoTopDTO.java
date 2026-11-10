package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;

public record ProductoTopDTO(
        Integer id,
        String nombre,
        String categoria,
        Integer vendidos,
        BigDecimal precio,
        Double porcentaje
) {}
