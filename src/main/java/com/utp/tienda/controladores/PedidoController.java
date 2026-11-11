package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.CloudinaryService;
import com.utp.tienda.modelos.servicios.PedidoService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.vistas.dto.CrearPedidoDTO;
import com.utp.tienda.vistas.dto.PedidoDTO;
import com.utp.tienda.vistas.dto.SubidaImagenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidos;
    private final SesionService sesiones;
    private final CloudinaryService cloudinary;

    public PedidoController(PedidoService pedidos, SesionService sesiones, CloudinaryService cloudinary) {
        this.pedidos = pedidos;
        this.sesiones = sesiones;
        this.cloudinary = cloudinary;
    }

    @PostMapping(value = "/comprobante-pago", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirComprobantePago(@RequestHeader(value = "Authorization", required = false) String auth,
                                                  @RequestPart("archivo") MultipartFile archivo) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Adjunta una captura válida de la transferencia."));
        }
        String tipoContenido = archivo.getContentType();
        if (tipoContenido != null && !tipoContenido.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El comprobante debe ser una imagen (JPG, PNG o WEBP)."));
        }
        try {
            Map<String, Object> resultado = cloudinary.subir(archivo, "techstore/pagos");
            Object publicIdObj = resultado.get("public_id");
            String publicId = publicIdObj != null ? publicIdObj.toString() : null;
            Object urlObj = resultado.get("secure_url");
            String url = urlObj != null ? urlObj.toString() : (resultado.get("url") != null ? resultado.get("url").toString() : null);
            return ResponseEntity.ok(new SubidaImagenResponse(url, publicId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo subir la captura del comprobante a Cloudinary."));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @Valid @RequestBody CrearPedidoDTO dto) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        try {
            PedidoDTO creado = pedidos.crear(id, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mis-compras")
    public ResponseEntity<?> misCompras(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        List<PedidoDTO> lista = pedidos.misCompras(id);
        return ResponseEntity.ok(lista);
    }

    @GetMapping
    public ResponseEntity<?> listarTodos(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        return ResponseEntity.ok(pedidos.listarTodos());
    }

    @PutMapping("/{numero}/estado")
    public ResponseEntity<?> actualizarEstado(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable String numero,
                                              @RequestBody Map<String, String> body) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado requerido"));
        }
        try {
            PedidoDTO actualizado = pedidos.actualizarEstado(id, numero, nuevoEstado);
            return ResponseEntity.ok(actualizado);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{numero}/pago")
    public ResponseEntity<?> confirmarPago(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @PathVariable String numero,
                                           @RequestBody Map<String, Object> body) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
        boolean aprobado = Boolean.TRUE.equals(body.get("aprobado"));
        try {
            return ResponseEntity.ok(pedidos.confirmarPagoTransferencia(id, numero, aprobado));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
