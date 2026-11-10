package com.utp.tienda.vistas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CrearPedidoDTO(
        @NotEmpty List<@Valid ItemPedidoDTO> items,
        @NotNull BigDecimal total,
        @Size(max = 30) String metodoPago,
        @Valid ComprobanteDTO comprobante,
        @Size(max = 30) String tipoEntrega,
        BigDecimal costoEnvio,
        @Size(max = 120) String correoContacto,
        @Size(max = 20) String celularContacto,
        @Size(max = 200) String direccionEnvio,
        @Size(max = 200) String referenciaEnvio,
        @Size(max = 100) String distritoEnvio
) {}
