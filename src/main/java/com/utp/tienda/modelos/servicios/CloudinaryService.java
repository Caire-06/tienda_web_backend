package com.utp.tienda.modelos.servicios;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

/**
 * Servicio de Integración con Cloudinary para almacenamiento de imágenes de productos.
 * Las credenciales se configuran por variables de entorno (nunca en el código).
 */
@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    private Cloudinary crearCloudinary() {
        String name = cloudName != null ? cloudName.trim() : "";
        String key = apiKey != null ? apiKey.trim() : "";
        String secret = apiSecret != null ? apiSecret.trim() : "";
        if (!name.isBlank() && !key.isBlank() && !secret.isBlank()) {
            return new Cloudinary(Map.of(
                    "cloud_name", name,
                    "api_key", key,
                    "api_secret", secret
            ));
        }
        String url = cloudinaryUrl != null ? cloudinaryUrl.trim() : "";
        if (!url.isBlank()) {
            return new Cloudinary(url);
        }
        throw new IllegalStateException(
                "Faltan credenciales de Cloudinary (CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET o CLOUDINARY_URL)."
        );
    }

    /** Sube el archivo a Cloudinary (carpeta techstore/productos) y devuelve { url, publicId }. */
    public Map<String, Object> subir(MultipartFile archivo) {
        return subir(archivo, "techstore/productos");
    }

    /** Sube un archivo a la carpeta indicada en Cloudinary y devuelve { url, publicId }. */
    public Map<String, Object> subir(MultipartFile archivo, String carpeta) {
        try {
            Cloudinary cloudinary = crearCloudinary();
            String destino = (carpeta != null && !carpeta.isBlank()) ? carpeta.trim() : "techstore/productos";
            return cloudinary.uploader().upload(
                    archivo.getBytes(),
                    Map.of("folder", destino)
            );
        } catch (Exception e) {
            throw new RuntimeException("No se pudo subir la imagen a Cloudinary: " + e.getMessage(), e);
        }
    }

    /** Sube un archivo (nombre + bytes) a Cloudinary (carpeta techstore/productos) y devuelve { url, publicId }. */
    public Map<String, Object> subir(String nombreArchivo, byte[] contenido) {
        try {
            Cloudinary cloudinary = crearCloudinary();
            String base64 = Base64.getEncoder().encodeToString(contenido);
            return cloudinary.uploader().upload("data:image/webp;base64," + base64, Map.of("folder", "techstore/productos", "original_filename", nombreArchivo));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo subir la imagen a Cloudinary: " + e.getMessage(), e);
        }
    }

    /** Elimina un recurso de imagen en Cloudinary a partir de su publicId. (Best effort) */
    public void destruir(String publicId) {
        try {
            if (publicId == null || publicId.isBlank()) return;
            Cloudinary cloudinary = crearCloudinary();
            cloudinary.uploader().destroy(publicId, Map.of("resource_type", "image"));
        } catch (Exception e) {
            // No romper el flujo principal si falla la limpieza en Cloudinary.
            System.err.println("[Cloudinary] Error al eliminar imagen: " + e.getMessage());
        }
    }
}
