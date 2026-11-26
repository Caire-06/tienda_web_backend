package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento único de inventario inicial por producto (relación 1:1).
 *
 * La cantidad se toma del campo existente "Stock físico" del producto al darlo de alta
 * (o de su stock actual al regularizar un producto ya existente). No se crea un campo
 * nuevo de cantidad en el formulario ni se duplica el precio de venta.
 *
 * Guardar la cantidad y la fecha en el evento es necesario para que el Kardex histórico
 * no se recalcule cuando el stock del producto cambia con compras/ventas posteriores.
 */
@Entity
@Table(name = "inventarios_iniciales")
public class InventarioInicial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario_inicial")
    private Integer idInventarioInicial;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    /** Fecha del evento: alta del producto o fecha de regularización de uno existente. */
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    // ===== Auditoría obligatoria TechStore =====
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "usuario_registro", length = 50)
    private String usuarioRegistro = "SISTEMA";

    public InventarioInicial() {}

    public InventarioInicial(Producto producto, Integer cantidad, BigDecimal costoUnitario,
                             LocalDateTime fecha, String usuarioRegistro) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.fecha = fecha;
        this.usuarioRegistro = (usuarioRegistro == null || usuarioRegistro.isBlank())
                ? "SISTEMA" : usuarioRegistro;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaCreacion == null) this.fechaCreacion = ahora;
        if (this.fechaActualizacion == null) this.fechaActualizacion = ahora;
        if (this.fecha == null) this.fecha = ahora;
        if (this.activo == null) this.activo = true;
        if (this.usuarioRegistro == null || this.usuarioRegistro.isBlank()) this.usuarioRegistro = "SISTEMA";
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdInventarioInicial() { return idInventarioInicial; }
    public void setIdInventarioInicial(Integer idInventarioInicial) { this.idInventarioInicial = idInventarioInicial; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getProductoId() { return producto != null ? producto.getIdProducto() : null; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Boolean getActivo() { return activo != null && activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
