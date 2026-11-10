package com.utp.tienda.vistas.dto;

import java.math.BigDecimal;
import java.util.List;

public record PedidoDTO(
        String numero,
        String fecha,
        String cliente,
        String estado,
        String tipoEntrega,
        String metodoPago,
        BigDecimal total,
        List<ItemPedidoDTO> items,
        ComprobanteDTO comprobante,
        String codigoLoteDespacho,
        String empresaTransporte
) {
    public PedidoDTO(String numero, String fecha, String cliente, String estado, String tipoEntrega, String metodoPago, BigDecimal total, List<ItemPedidoDTO> items, ComprobanteDTO comprobante) {
        this(numero, fecha, cliente, estado, tipoEntrega, metodoPago, total, items, comprobante, null, null);
    }
}
