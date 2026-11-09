package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;

/** Respuesta de la subida de imagen a Cloudinary. */
public record SubidaImagenResponse(String url, String publicId) {}
