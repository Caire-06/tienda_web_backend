package com.utp.tienda.modelos.servicios;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio centralizado de protección contra abuso y agotamiento de APIs externas gratuitas:
 * - Ventanas deslizantes (rate limiting por IP/token compatible con proxy X-Forwarded-For de Render).
 * - Circuit breaker con tope diario configurable para Brevo, APIPeru/SUNAT y Cloudinary.
 */
@Service
public class ControlTraficoService {

    public static class LimiteCuotaDiariaException extends IllegalStateException {
        public LimiteCuotaDiariaException(String message) {
            super(message);
        }
    }

    public record ResultadoLimite(boolean permitido, long segundosEspera) {}

    private static final int MAX_BUCKETS_MEMORIA = 4096;
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private static final class ContadorDiario {
        private LocalDate fecha = LocalDate.now(ZONA_PERU);
        private final AtomicInteger cantidad = new AtomicInteger(0);

        synchronized boolean intentarIncrementar(int limite) {
            if (limite <= 0) return true;
            LocalDate hoy = LocalDate.now(ZONA_PERU);
            if (!hoy.equals(this.fecha)) {
                this.fecha = hoy;
                this.cantidad.set(0);
            }
            if (this.cantidad.get() >= limite) {
                return false;
            }
            this.cantidad.incrementAndGet();
            return true;
        }

        synchronized int actual() {
            LocalDate hoy = LocalDate.now(ZONA_PERU);
            return hoy.equals(this.fecha) ? this.cantidad.get() : 0;
        }
    }

    private final ConcurrentHashMap<String, Deque<Long>> ventanas = new ConcurrentHashMap<>();
    private final ContadorDiario contadorBrevo = new ContadorDiario();
    private final ContadorDiario contadorConsultasFiscales = new ContadorDiario();
    private final ContadorDiario contadorCloudinary = new ContadorDiario();

    private final int limiteDiarioBrevo;
    private final int limiteDiarioConsultas;
    private final int limiteDiarioCloudinary;

    public ControlTraficoService(
            @Value("${app.limites.brevo-diario:250}") int limiteDiarioBrevo,
            @Value("${app.limites.consultas-fiscales-diario:400}") int limiteDiarioConsultas,
            @Value("${app.limites.cloudinary-diario:150}") int limiteDiarioCloudinary) {
        this.limiteDiarioBrevo = limiteDiarioBrevo;
        this.limiteDiarioConsultas = limiteDiarioConsultas;
        this.limiteDiarioCloudinary = limiteDiarioCloudinary;
    }

    public ResultadoLimite intentarConsumir(String claveRegla, String identificadorCliente,
                                            int maximoSolicitudes, long ventanaSegundos) {
        long ahoraMs = System.currentTimeMillis();
        long ventanaMs = ventanaSegundos * 1000L;
        long limiteInferiorMs = ahoraMs - ventanaMs;

        if (ventanas.size() >= MAX_BUCKETS_MEMORIA) {
            depurarVentanasAntiguas(ahoraMs - 3600_000L);
        }

        String claveCompuesta = claveRegla + "|" + identificadorCliente;
        Deque<Long> cola = ventanas.computeIfAbsent(claveCompuesta, k -> new ArrayDeque<>());

        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst() <= limiteInferiorMs) {
                cola.pollFirst();
            }
            if (cola.size() >= maximoSolicitudes) {
                long masAntiguaMs = cola.peekFirst() != null ? cola.peekFirst() : ahoraMs;
                long restanteMs = Math.max(1000L, (masAntiguaMs + ventanaMs) - ahoraMs);
                long segundos = (restanteMs + 999L) / 1000L;
                return new ResultadoLimite(false, segundos);
            }
            cola.addLast(ahoraMs);
            return new ResultadoLimite(true, 0);
        }
    }

    public void consumirCuotaDiariaBrevo() {
        if (!contadorBrevo.intentarIncrementar(limiteDiarioBrevo)) {
            throw new LimiteCuotaDiariaException(
                    "Se alcanzó temporalmente el límite preventivo diario de envíos de correo. Intenta nuevamente mañana.");
        }
    }

    public void consumirCuotaDiariaConsultasFiscales() {
        if (!contadorConsultasFiscales.intentarIncrementar(limiteDiarioConsultas)) {
            throw new LimiteCuotaDiariaException(
                    "Se alcanzó temporalmente el límite diario de consultas externas RENIEC/SUNAT. Completa los datos manualmente.");
        }
    }

    public void consumirCuotaDiariaCloudinary() {
        if (!contadorCloudinary.intentarIncrementar(limiteDiarioCloudinary)) {
            throw new LimiteCuotaDiariaException(
                    "Se alcanzó temporalmente el límite preventivo diario de carga de imágenes.");
        }
    }

    public int getEnviosBrevoHoy() {
        return contadorBrevo.actual();
    }

    public String obtenerIpCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String primera = forwarded.split(",")[0].trim();
            if (!primera.isBlank() && primera.length() <= 45) {
                return primera;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && realIp.length() <= 45) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : "desconocida";
    }

    private void depurarVentanasAntiguas(long umbralMs) {
        Iterator<Map.Entry<String, Deque<Long>>> it = ventanas.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Long>> entry = it.next();
            Deque<Long> q = entry.getValue();
            synchronized (q) {
                if (q.isEmpty() || q.peekLast() < umbralMs) {
                    it.remove();
                }
            }
        }
    }
}

