package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;

/** Movimiento del Kardex (entrada o salida de mercadería). */
public record KardexMovimientoDTO(
        String id,
        String fecha,
        String tipo,
        Integer productoId,
        String producto,
        Integer cantidad,
        BigDecimal valorUnitario,
        Integer stockAnterior,
        Integer stockNuevo,
        String motivo,
        String usuario
) {}
