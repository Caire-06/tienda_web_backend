package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Especificación técnica de un producto (relación 1:N en 3FN).
 *
 * La magnitud y el valor cualitativo se guardan juntos en {@code valor}
 * (por ejemplo "Memoria RAM" = "8 GB"), de modo que no existen columnas
 * nulas por categoría ni una columna {@code unidad_medida} separada.
 */
@Entity
@Table(name = "especificaciones_tecnicas")
public class EspecificacionTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especificacion")
    private Integer idEspecificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "caracteristica", nullable = false, length = 100)
    private String caracteristica;

    @Column(name = "valor", nullable = false, length = 150)
    private String valor;

    // ===== Auditoría obligatoria TechStore =====
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "usuario_registro", nullable = false, length = 50)
    private String usuarioRegistro = "SISTEMA";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public EspecificacionTecnica() {}

    public EspecificacionTecnica(Producto producto, String caracteristica, String valor, String usuarioRegistro) {
        this.producto = producto;
        this.caracteristica = caracteristica;
        this.valor = valor;
        this.usuarioRegistro = usuarioRegistro;
        this.activo = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = ahora;
        }
        if (this.fechaActualizacion == null) {
            this.fechaActualizacion = ahora;
        }
        if (this.usuarioRegistro == null || this.usuarioRegistro.isBlank()) {
            this.usuarioRegistro = "SISTEMA";
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdEspecificacion() { return idEspecificacion; }
    public void setIdEspecificacion(Integer idEspecificacion) { this.idEspecificacion = idEspecificacion; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getCaracteristica() { return caracteristica; }
    public void setCaracteristica(String caracteristica) { this.caracteristica = caracteristica; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public Boolean getActivo() { return activo != null && activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
