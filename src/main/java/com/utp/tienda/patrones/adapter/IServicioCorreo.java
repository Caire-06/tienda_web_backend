package com.utp.tienda.patrones.adapter;

/**
 * Interfaz objetivo del patrón Adapter para el envío de correos transaccionales.
 * Permite desacoplar la lógica de autenticación del proveedor externo (Brevo).
 */
public interface IServicioCorreo {

    boolean estaConfigurado();

    void enviarCorreo(String destinatarioEmail, String destinatarioNombre, String asunto, String htmlContenido);
}

