package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CrearDespachoLoteDTO(
        @NotEmpty(message = "Debe seleccionar al menos un pedido para despachar")
        List<String> numerosPedidos,

        @NotBlank(message = "La empresa de transporte o courier es obligatoria")
        String empresaTransporte,

        String numeroGuiaRemision,
        String conductor,
        String placaVehiculo,
        String observaciones
) {}

