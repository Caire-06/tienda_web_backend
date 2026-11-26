package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import java.util.List;

/** DTO de respuesta para productos (incluye su ficha técnica en 3FN). */
public record ProductoDTO(
        Integer id,
        String nombre,
        String categoria,
        BigDecimal precio,
        Integer stock,
        Integer vendidos,
        String imagen,
        String descripcion,
        Boolean activo,
        String marca,
        String fechaCreacion,
        List<EspecificacionDTO> especificaciones,
        BigDecimal costoUnitarioInicial,
        String fechaInventarioInicial
) {}
