package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import java.util.List;

public record EntradaDTO(
        Integer id,
        String fecha,
        List<ItemEntradaDTO> items
) {}
