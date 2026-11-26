package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;

/** Movimiento del Kardex (entrada o salida de mercadería) con saldos valorizados y promedio móvil. */
public record KardexMovimientoDTO(
        String id,
        String fecha,
        String tipo,
        Integer productoId,
        String producto,
        Integer cantidad,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        Integer stockAnterior,
        Integer stockNuevo,
        BigDecimal saldoValorAnterior,
        BigDecimal saldoValorNuevo,
        BigDecimal saldoPromedio,
        String guia,
        String factura,
        String detalle,
        String motivo,
        String usuario,
        /** true cuando el saldo es negativo o una salida no pudo valorizarse con costo real. */
        Boolean inconsistente,
        String observacion
) {}
