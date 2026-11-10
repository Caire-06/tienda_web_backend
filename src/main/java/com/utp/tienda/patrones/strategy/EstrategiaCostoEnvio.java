package com.utp.tienda.patrones.strategy;

import java.math.BigDecimal;

/**
 * Patrón de Diseño GoF Strategy para cálculo de costos de envío y fletes.
 */
public interface EstrategiaCostoEnvio {

    BigDecimal UMBRAL_ENVIO_GRATIS = new BigDecimal("499.00");
    BigDecimal TARIFA_DELIVERY_PROPIO = new BigDecimal("15.00");

    BigDecimal calcular(BigDecimal subtotalProductos);

    static EstrategiaCostoEnvio obtenerPorModalidad(String tipoEntrega) {
        if (tipoEntrega == null) return subtotal -> BigDecimal.ZERO;
        return switch (tipoEntrega.toLowerCase().trim()) {
            case "delivery_propio", "envio" -> subtotal -> {
                if (subtotal == null) return TARIFA_DELIVERY_PROPIO;
                return subtotal.compareTo(UMBRAL_ENVIO_GRATIS) >= 0 ? BigDecimal.ZERO : TARIFA_DELIVERY_PROPIO;
            };
            case "agencia_destino", "recojo" -> subtotal -> BigDecimal.ZERO;
            default -> subtotal -> BigDecimal.ZERO;
        };
    }
}
