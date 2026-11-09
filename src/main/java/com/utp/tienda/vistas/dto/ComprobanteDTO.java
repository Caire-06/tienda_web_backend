package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ComprobanteDTO(
        @NotBlank @Size(max = 20) String tipo,
        @NotBlank @Size(max = 20) String documento,
        @NotBlank @Size(max = 200) String nombreRazonSocial,
        @Size(max = 250) String direccion,
        @NotNull BigDecimal subtotal,
        @NotNull BigDecimal igv,
        @NotNull BigDecimal total
) {}
