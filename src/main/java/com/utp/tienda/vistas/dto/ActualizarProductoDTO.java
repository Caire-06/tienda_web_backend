package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO para actualizar (parcial) un producto. */
public record ActualizarProductoDTO(
        @Size(max = 200) String nombre,
        @Size(max = 50) String categoria,
        @Positive BigDecimal precio,
        @Min(0) Integer stock,
        @Size(max = 300) String imagen,
        @Size(max = 500) String descripcion,
        @Min(0) Integer vendidos,
        Boolean activo,
        @Size(max = 100) String marca,
        @Valid List<@Valid EspecificacionDTO> especificaciones,
        /** Solo se aplica a productos sin inventario inicial registrado (regularización). */
        @DecimalMin(value = "0", message = "El costo unitario inicial no puede ser negativo") BigDecimal costoUnitarioInicial
) {}
