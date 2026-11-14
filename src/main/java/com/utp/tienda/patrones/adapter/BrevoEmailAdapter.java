package com.utp.tienda.patrones.adapter;

import com.utp.tienda.modelos.servicios.ControlTraficoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Adaptador concreto para Brevo Transactional Email API v3 sobre HTTPS (puerto 443),
 * compatible con Render Free Tier sin usar puertos SMTP bloqueados.
 */
@Service
public class BrevoEmailAdapter implements IServicioCorreo {

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final ControlTraficoService controlTraficoService;

    public BrevoEmailAdapter(String apiKey, String senderEmail, String senderName) {
        this(apiKey, senderEmail, senderName, null);
    }

    @Autowired
    public BrevoEmailAdapter(
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.sender-email:}") String senderEmail,
            @Value("${brevo.sender-name:TechStore}") String senderName,
            ControlTraficoService controlTraficoService) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail != null ? senderEmail.trim() : "";
        this.senderName = (senderName != null && !senderName.isBlank()) ? senderName.trim() : "TechStore";
        this.controlTraficoService = controlTraficoService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean estaConfigurado() {
        return !apiKey.isBlank() && !senderEmail.isBlank();
    }

    @Override
    public void enviarCorreo(String destinatarioEmail, String destinatarioNombre, String asunto, String htmlContenido) {
        if (!estaConfigurado()) {
            throw new IllegalStateException(
                    "El servicio de correo (Brevo) no está configurado. Verifica BREVO_API_KEY y BREVO_SENDER_EMAIL.");
        }
        if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
            throw new IllegalArgumentException("El correo del destinatario es obligatorio.");
        }
        if (controlTraficoService != null) {
            controlTraficoService.consumirCuotaDiariaBrevo();
        }

        String nombreDestino = (destinatarioNombre != null && !destinatarioNombre.isBlank())
                ? destinatarioNombre.trim()
                : destinatarioEmail.trim();

        String payload = new StringBuilder(512)
                .append("{\"sender\":{\"name\":\"").append(escaparJson(senderName))
                .append("\",\"email\":\"").append(escaparJson(senderEmail)).append("\"},")
                .append("\"to\":[{\"email\":\"").append(escaparJson(destinatarioEmail.trim()))
                .append("\",\"name\":\"").append(escaparJson(nombreDestino)).append("\"}],")
                .append("\"subject\":\"").append(escaparJson(asunto)).append("\",")
                .append("\"htmlContent\":\"").append(escaparJson(htmlContenido)).append("\"}")
                .toString();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json; charset=UTF-8")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                System.err.println("[Brevo] Error HTTP " + status + " al enviar correo a " + destinatarioEmail);
                throw new IllegalStateException("No se pudo enviar el correo de verificación en este momento (código " + status + ").");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("[Brevo] Fallo de comunicación: " + ex.getMessage());
            throw new IllegalStateException("Error al comunicarse con el proveedor de correo. Intenta nuevamente.");
        }
    }

    private static String escaparJson(String valor) {
        if (valor == null) return "";
        StringBuilder sb = new StringBuilder(valor.length() + 16);
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}

