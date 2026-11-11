package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.ComprobantePedido;
import com.utp.tienda.modelos.entidades.DatosPersonales;
import com.utp.tienda.modelos.repositorios.ComprobantePedidoRepository;
import com.utp.tienda.modelos.repositorios.DatosPersonalesRepository;
import com.utp.tienda.vistas.dto.ConsultaDniDTO;
import com.utp.tienda.vistas.dto.ConsultaRucDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Patrón Estructural: Adapter
 * Unifica consultas RENIEC (DNI) y SUNAT (RUC) entre registros propios en 3FN,
 * caché temporal en memoria (24h) y proveedores externos protegidos por cuota diaria.
 */
@Service
public class ConsultaFiscalService {

    public static class TokenNoConfiguradoException extends RuntimeException {
        public TokenNoConfiguradoException(String message) {
            super(message);
        }
    }

    public static class DocumentoNoEncontradoException extends RuntimeException {
        public DocumentoNoEncontradoException(String message) {
            super(message);
        }
    }

    private record EntradaCache<T>(T valor, long expiraEnMs) {}

    private static final long TTL_CACHE_MS = Duration.ofHours(24).toMillis();
    private static final int MAX_CACHE_DOCUMENTOS = 1000;

    private final DatosPersonalesRepository datosPersonalesRepository;
    private final ComprobantePedidoRepository comprobantePedidoRepository;
    private final ControlTraficoService controlTraficoService;
    private final HttpClient httpClient;
    private final Map<String, EntradaCache<ConsultaDniDTO>> cacheDni = crearMapaLru();
    private final Map<String, EntradaCache<ConsultaRucDTO>> cacheRuc = crearMapaLru();

    @Value("${apisperu.token:}")
    private String apisperuToken;

    @Value("${apisnetpe.token:}")
    private String apisnetpeToken;

    public ConsultaFiscalService(DatosPersonalesRepository datosPersonalesRepository,
                                 ComprobantePedidoRepository comprobantePedidoRepository,
                                 ControlTraficoService controlTraficoService) {
        this.datosPersonalesRepository = datosPersonalesRepository;
        this.comprobantePedidoRepository = comprobantePedidoRepository;
        this.controlTraficoService = controlTraficoService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private static <T> Map<String, EntradaCache<T>> crearMapaLru() {
        return Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, EntradaCache<T>> eldest) {
                return size() > MAX_CACHE_DOCUMENTOS;
            }
        });
    }

    private <T> Optional<T> leerCache(Map<String, EntradaCache<T>> cache, String clave) {
        EntradaCache<T> entrada = cache.get(clave);
        if (entrada == null) return Optional.empty();
        if (System.currentTimeMillis() > entrada.expiraEnMs()) {
            cache.remove(clave);
            return Optional.empty();
        }
        return Optional.of(entrada.valor());
    }

    private <T> void guardarCache(Map<String, EntradaCache<T>> cache, String clave, T valor) {
        cache.put(clave, new EntradaCache<>(valor, System.currentTimeMillis() + TTL_CACHE_MS));
    }

    @Transactional(readOnly = true)
    public ConsultaDniDTO consultarDni(String dniRaw) {
        String dni = dniRaw != null ? dniRaw.trim().replaceAll("\\D", "") : "";
        if (!dni.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos numéricos.");
        }

        // 1. Verificar coincidencia previa en PostgreSQL (3FN: datos_personales)
        Optional<DatosPersonales> local = datosPersonalesRepository.findByDni(dni);
        if (local.isPresent()) {
            DatosPersonales dp = local.get();
            String nombres = textoSeguro(dp.getNombres());
            String apellidos = textoSeguro(dp.getApellidos());
            String completo = (nombres + " " + apellidos).trim();
            if (!completo.isBlank()) {
                String[] partesAp = apellidos.split("\\s+", 2);
                String pat = partesAp.length > 0 ? partesAp[0] : "";
                String mat = partesAp.length > 1 ? partesAp[1] : "";
                return new ConsultaDniDTO(dni, completo, nombres, pat, mat, "REGISTRO_LOCAL");
            }
        }

        // 2. Verificar caché temporal de 24h para evitar llamadas repetidas
        Optional<ConsultaDniDTO> enCache = leerCache(cacheDni, dni);
        if (enCache.isPresent()) {
            return enCache.get();
        }

        String tokenPeru = textoSeguro(apisperuToken);
        String tokenNetPe = textoSeguro(apisnetpeToken);
        if (tokenPeru.isBlank() && tokenNetPe.isBlank()) {
            throw new TokenNoConfiguradoException(
                    "No hay token externo configurado en el servidor (APIPERU_TOKEN / APISNETPE_TOKEN) para consultar RENIEC.");
        }

        // 3. Aplicar tope diario preventivo solo antes de consumir API externa
        controlTraficoService.consumirCuotaDiariaConsultasFiscales();

        // 4. Consultar APIsPeru si tiene token configurado en el servidor
        if (!tokenPeru.isBlank()) {
            Map<String, Object> json = ejecutarGet(
                    "https://dniruc.apisperu.com/api/v1/dni/" + dni + "?token=" + tokenPeru,
                    null,
                    "DNI " + dni + " no encontrado en RENIEC."
            );
            String nombres = textoCampo(json, "nombres");
            String pat = textoCampo(json, "apellidoPaterno");
            String mat = textoCampo(json, "apellidoMaterno");
            String completo = textoCampo(json, "nombreCompleto");
            if (completo.isBlank()) {
                completo = (nombres + " " + pat + " " + mat).trim();
            }
            if (!completo.isBlank()) {
                ConsultaDniDTO resultado = new ConsultaDniDTO(dni, completo, nombres, pat, mat, "RENIEC");
                guardarCache(cacheDni, dni, resultado);
                return resultado;
            }
        }

        // 5. Consultar APIs.net.pe v2 si tiene token configurado en el servidor
        if (!tokenNetPe.isBlank()) {
            Map<String, Object> json = ejecutarGet(
                    "https://api.apis.net.pe/v2/reniec/dni?numero=" + dni,
                    "Bearer " + tokenNetPe,
                    "DNI " + dni + " no encontrado en RENIEC."
            );
            String nombres = textoCampo(json, "nombres");
            String pat = textoCampo(json, "apellidoPaterno");
            String mat = textoCampo(json, "apellidoMaterno");
            String completo = textoCampo(json, "nombreCompleto");
            if (completo.isBlank()) {
                completo = (nombres + " " + pat + " " + mat).trim();
            }
            if (!completo.isBlank()) {
                ConsultaDniDTO resultado = new ConsultaDniDTO(dni, completo, nombres, pat, mat, "RENIEC");
                guardarCache(cacheDni, dni, resultado);
                return resultado;
            }
        }

        throw new DocumentoNoEncontradoException("DNI " + dni + " no encontrado en RENIEC.");
    }

    @Transactional(readOnly = true)
    public ConsultaRucDTO consultarRuc(String rucRaw) {
        String ruc = rucRaw != null ? rucRaw.trim().replaceAll("\\D", "") : "";
        if (!ruc.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener exactamente 11 dígitos numéricos.");
        }
        if (!Set.of("10", "15", "17", "20").contains(ruc.substring(0, 2))) {
            throw new IllegalArgumentException("El RUC debe iniciar con 10, 15, 17 o 20.");
        }

        // 1. Verificar si ya existe un comprobante previo emitido a ese RUC
        Optional<ComprobantePedido> local = comprobantePedidoRepository.findFirstByDocumentoOrderByIdComprobanteDesc(ruc);
        if (local.isPresent()) {
            ComprobantePedido cp = local.get();
            String razon = textoSeguro(cp.getNombreRazonSocial());
            if (!razon.isBlank()) {
                String dir = textoSeguro(cp.getDireccionFiscal());
                return new ConsultaRucDTO(ruc, razon, "ACTIVO", "HABIDO",
                        dir.isBlank() ? "LIMA - PERÚ" : dir, "REGISTRO_LOCAL");
            }
        }

        // 2. Verificar caché temporal de 24h para evitar llamadas repetidas
        Optional<ConsultaRucDTO> enCache = leerCache(cacheRuc, ruc);
        if (enCache.isPresent()) {
            return enCache.get();
        }

        String tokenPeru = textoSeguro(apisperuToken);
        String tokenNetPe = textoSeguro(apisnetpeToken);
        if (tokenPeru.isBlank() && tokenNetPe.isBlank()) {
            throw new TokenNoConfiguradoException(
                    "No hay token externo configurado en el servidor (APIPERU_TOKEN / APISNETPE_TOKEN) para consultar SUNAT.");
        }

        // 3. Aplicar tope diario preventivo solo antes de consumir API externa
        controlTraficoService.consumirCuotaDiariaConsultasFiscales();

        // 4. Consultar APIsPeru si tiene token configurado en el servidor
        if (!tokenPeru.isBlank()) {
            Map<String, Object> json = ejecutarGet(
                    "https://dniruc.apisperu.com/api/v1/ruc/" + ruc + "?token=" + tokenPeru,
                    null,
                    "RUC " + ruc + " no encontrado en SUNAT."
            );
            String razon = textoCampo(json, "razonSocial");
            if (!razon.isBlank()) {
                ConsultaRucDTO resultado = construirRucDto(ruc, razon, json);
                guardarCache(cacheRuc, ruc, resultado);
                return resultado;
            }
        }

        // 5. Consultar APIs.net.pe v2 si tiene token configurado en el servidor
        if (!tokenNetPe.isBlank()) {
            Map<String, Object> json = ejecutarGet(
                    "https://api.apis.net.pe/v2/sunat/ruc?numero=" + ruc,
                    "Bearer " + tokenNetPe,
                    "RUC " + ruc + " no encontrado en SUNAT."
            );
            String razon = textoCampo(json, "razonSocial");
            if (razon.isBlank()) razon = textoCampo(json, "nombre");
            if (!razon.isBlank()) {
                ConsultaRucDTO resultado = construirRucDto(ruc, razon, json);
                guardarCache(cacheRuc, ruc, resultado);
                return resultado;
            }
        }

        throw new DocumentoNoEncontradoException("RUC " + ruc + " no encontrado en SUNAT.");
    }

    private Map<String, Object> ejecutarGet(String url, String authorizationHeader, String msgNoEncontrado) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .GET();
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                builder.header("Authorization", authorizationHeader);
            }
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status == 401 || status == 403) {
                throw new IllegalStateException("El token externo para consultas RENIEC/SUNAT fue rechazado (" + status + ").");
            }
            if (status == 429) {
                throw new IllegalStateException("Se alcanzó el límite de consultas del servicio externo.");
            }
            if (status == 404 || status == 422) {
                throw new DocumentoNoEncontradoException(msgNoEncontrado);
            }
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("El servicio externo respondió con estado " + status + ".");
            }
            Map<String, Object> parsed = JsonParserFactory.getJsonParser().parseMap(resp.body());
            if (Boolean.FALSE.equals(parsed.get("success"))) {
                String msgApi = textoCampo(parsed, "message");
                if (msgApi.toLowerCase().contains("token")) {
                    throw new IllegalStateException("El token externo para consultas fue rechazado: " + msgApi);
                }
                throw new DocumentoNoEncontradoException(msgApi.isBlank() ? msgNoEncontrado : msgApi);
            }
            return parsed;
        } catch (DocumentoNoEncontradoException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo conectar con el servicio externo RENIEC/SUNAT.");
        }
    }

    private ConsultaRucDTO construirRucDto(String ruc, String razon, Map<String, Object> json) {
        String estado = textoCampo(json, "estado");
        String condicion = textoCampo(json, "condicion");
        String dir = textoCampo(json, "direccion");
        if (dir.isBlank() || "-".equals(dir)) {
            String ubigeo = java.util.stream.Stream.of(
                            textoCampo(json, "distrito"),
                            textoCampo(json, "provincia"),
                            textoCampo(json, "departamento")
                    )
                    .filter(s -> !s.isBlank() && !"-".equals(s))
                    .collect(java.util.stream.Collectors.joining(", "));
            dir = ubigeo.isBlank() ? "LIMA - PERÚ" : ubigeo;
        }
        return new ConsultaRucDTO(
                ruc,
                razon,
                estado.isBlank() ? "ACTIVO" : estado,
                condicion.isBlank() ? "HABIDO" : condicion,
                dir,
                "SUNAT"
        );
    }

    private static String textoSeguro(String valor) {
        return valor != null ? valor.trim() : "";
    }

    private static String textoCampo(Map<String, Object> json, String clave) {
        Object val = json.get(clave);
        return val != null ? String.valueOf(val).trim() : "";
    }
}
