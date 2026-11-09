package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemPedidoDTO(
        @NotNull Integer id,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull BigDecimal precio,
        @NotNull @Min(1) Integer cantidad,
        @Size(max = 300) String imagen
) {}
