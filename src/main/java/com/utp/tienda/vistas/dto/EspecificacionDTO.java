package com.utp.tienda.vistas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de una especificación técnica.
 * El valor ya incluye su magnitud (ej. "8 GB", "30 horas"), por lo que no
 * existe un campo de unidad de medida separado (evita valores NULL).
 */
public record EspecificacionDTO(
        @NotBlank @Size(max = 100) String caracteristica,
        @NotBlank @Size(max = 150) String valor
) {}
