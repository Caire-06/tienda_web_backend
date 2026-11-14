package com.utp.tienda.modelos.servicios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Servicio de validación para Cloudflare Turnstile (CAPTCHA inteligente).
 * Protege los endpoints públicos que disparan envíos de correo (/registro y /recuperar-password).
 *
 * Si TURNSTILE_SECRET_KEY no está configurada, opera en modo permisivo (bypass)
 * para no bloquear el desarrollo local o pruebas académicas.
 * En cuanto se define la clave secreta, la validación se vuelve estricta (fail-closed).
 */
@Service
public class TurnstileCaptchaService {

    public static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final String secretKey;
    private final HttpClient httpClient;

    public TurnstileCaptchaService() {
        this("");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TurnstileCaptchaService(@Value("${turnstile.secret-key:}") String secretKey) {
        this(secretKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());
    }

    public TurnstileCaptchaService(String secretKey, HttpClient httpClient) {
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.httpClient = httpClient;
    }

    /** Indica si la validación contra Cloudflare está activa. */
    public boolean estaHabilitado() {
        return !secretKey.isBlank();
    }

    /**
     * Valida el token emitido por el widget de Cloudflare Turnstile en el frontend.
     *
     * @param captchaToken Token de respuesta obtenido por el cliente
     * @param ipCliente IP del cliente que realiza la petición (opcional)
     * @return true si es válido o si el servicio está deshabilitado en el entorno; false en caso contrario
     */
    public boolean validar(String captchaToken, String ipCliente) {
        if (!estaHabilitado()) {
            return true;
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }

        try {
            StringBuilder body = new StringBuilder();
            body.append("secret=").append(URLEncoder.encode(secretKey, StandardCharsets.UTF_8));
            body.append("&response=").append(URLEncoder.encode(captchaToken.trim(), StandardCharsets.UTF_8));
            if (ipCliente != null && !ipCliente.isBlank() && !"desconocida".equalsIgnoreCase(ipCliente)) {
                body.append("&remoteip=").append(URLEncoder.encode(ipCliente.trim(), StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                System.err.println("[Turnstile] HTTP " + response.statusCode() + " al verificar con Cloudflare: " + response.body());
                return false;
            }

            Map<String, Object> parsed = JsonParserFactory.getJsonParser().parseMap(response.body());
            return Boolean.TRUE.equals(parsed.get("success"));
        } catch (Exception e) {
            System.err.println("[Turnstile] Fallo al validar captcha: " + e.getMessage());
            return false;
        }
    }
}
