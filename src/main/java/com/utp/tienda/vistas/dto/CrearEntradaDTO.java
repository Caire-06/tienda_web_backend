package com.utp.tienda.vistas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** DTO para registrar una entrada de mercadería (cabecera + líneas). */
public record CrearEntradaDTO(
        @NotEmpty List<@Valid ItemEntradaDTO> items
) {}
