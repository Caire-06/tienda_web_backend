package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import java.util.List;

public record DespachoLoteDTO(
        Integer id,
        String codigoLote,
        String empresaTransporte,
        String numeroGuiaRemision,
        String conductor,
        String placaVehiculo,
        String observaciones,
        String fechaDespacho,
        String usuarioRegistro,
        String estado,
        Integer cantidadPedidos,
        BigDecimal totalValorizado,
        List<String> numerosPedidos
) {}
