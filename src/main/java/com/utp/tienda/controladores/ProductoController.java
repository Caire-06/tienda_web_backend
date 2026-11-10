package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.CloudinaryService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.modelos.servicios.ProductoService;
import com.utp.tienda.vistas.dto.ActualizarProductoDTO;
import com.utp.tienda.vistas.dto.CrearProductoDTO;
import com.utp.tienda.vistas.dto.ProductoDTO;
import com.utp.tienda.vistas.dto.SubidaImagenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {
    private final ProductoService productos;
    private final CloudinaryService cloudinary;
    private final SesionService sesiones;

    public ProductoController(ProductoService productos, CloudinaryService cloudinary, SesionService sesiones) {
        this.productos = productos;
        this.cloudinary = cloudinary;
        this.sesiones = sesiones;
    }

    private Integer usuarioIdO401(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        Integer id = sesiones.identificar(auth);
        if (id == null) return null;
        return id;
    }

    private ResponseEntity<Map<String, Object>> noAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sesión inválida"));
    }

    /** GET /api/productos -> listar productos (rol autenticado). */
    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listar(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) {
            List<ProductoDTO> vacia = List.of();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(vacia);
        }
        return ResponseEntity.ok(productos.listar());
    }

    /** GET /api/productos/{id} -> detalle del producto. */
    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Integer id,
                                    @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            return ResponseEntity.ok(productos.detalle(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/productos -> registrar producto (con imagen ya subida a Cloudinary). */
    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth,
                                   @Valid @org.springframework.web.bind.annotation.RequestBody CrearProductoDTO dto) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productos.crear(usuarioId, dto));
        } catch (IllegalArgumentException e) {
            // El service lanza IllegalArgumentException con rol no permitido
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/productos/{id} -> editar producto (con imagen ya subida a Cloudinary). */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                       @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth,
                                       @Valid @org.springframework.web.bind.annotation.RequestBody ActualizarProductoDTO dto) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            return ResponseEntity.ok(productos.actualizar(usuarioId, id, dto));
        } catch (IllegalArgumentException e) {
            // El service lanza IllegalArgumentException("Producto no encontrado")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/productos/{id} -> eliminar producto. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id,
                                     @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            productos.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/productos/seed -> registra los productos iniciales con sus imagenes subidas a Cloudinary. */
    @PostMapping("/seed")
    public ResponseEntity<?> seed(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            return ResponseEntity.ok(productos.registrarProductosIniciales());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/productos/imagen -> subir archivo a Cloudinary (multipart) y devolver URL + publicId. */
    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirImagen(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String auth,
                                         @org.springframework.web.bind.annotation.RequestPart("archivo") org.springframework.web.multipart.MultipartFile archivo) {
        Integer usuarioId = usuarioIdO401(auth);
        if (usuarioId == null) return noAutorizado();
        try {
            Map<String, Object> resultado = cloudinary.subir(archivo);
            Object publicIdObj = resultado.get("public_id");
            String publicId = publicIdObj != null ? publicIdObj.toString() : null;
            Object urlObj = resultado.get("secure_url");
            String url = urlObj != null ? urlObj.toString() : (resultado.get("url") != null ? resultado.get("url").toString() : null);
            return ResponseEntity.ok(new SubidaImagenResponse(url, publicId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al subir imagen a Cloudinary"));
        }
    }
}
