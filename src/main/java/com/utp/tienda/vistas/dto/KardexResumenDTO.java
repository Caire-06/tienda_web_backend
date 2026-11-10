package com.utp.tienda.vistas.dto;

import java.util.List;

/** Resumen del Kardex para el dashboard del almacén. */
public record KardexResumenDTO(
        long movimientos,
        long entradas,
        long salidas,
        int productosRegistrados,
        int stockTotal,
        int stockCriticoBajo,
        List<KardexMovimientoDTO> ultimosMovimientos,
        List<KardexMovimientoDTO> ultimasEntradas
) {}
