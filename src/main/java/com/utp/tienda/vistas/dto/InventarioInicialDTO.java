package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;

/** Inventario inicial de un producto, expuesto para regularización y auditoría. */
public record InventarioInicialDTO(
        Integer productoId,
        String producto,
        Integer cantidad,
        BigDecimal costoUnitario,
        String fecha,
        String usuarioRegistro
) {}
