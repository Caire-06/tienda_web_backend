package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "vendidos", nullable = false)
    private Integer vendidos;

    @Column(name = "imagen", length = 300)
    private String imagen;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /** Marca del dispositivo: atributo propio del producto (3FN), no una especificación. */
    @Column(name = "marca", length = 100)
    private String marca = "Genérico";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ===== Auditoría obligatoria TechStore =====
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "usuario_registro", length = 50)
    private String usuarioRegistro = "SISTEMA";

    /** Ficha técnica en 3FN: un producto tiene muchas especificaciones (1:N). */
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EspecificacionTecnica> especificaciones = new ArrayList<>();

    public Producto() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = ahora;
        }
        if (this.fechaActualizacion == null) {
            this.fechaActualizacion = ahora;
        }
        if (this.marca == null || this.marca.isBlank()) {
            this.marca = "Genérico";
        }
        if (this.usuarioRegistro == null || this.usuarioRegistro.isBlank()) {
            this.usuarioRegistro = "SISTEMA";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getVendidos() { return vendidos; }
    public void setVendidos(Integer vendidos) { this.vendidos = vendidos; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public Boolean getActivo() { return activo != null && activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public List<EspecificacionTecnica> getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(List<EspecificacionTecnica> especificaciones) { this.especificaciones = especificaciones; }
}
