package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_entradas")
public class DetalleEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_entrada")
    private Integer idDetalleEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrada", nullable = false)
    private Entrada entrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "guia", length = 50)
    private String guia;

    @Column(name = "factura", length = 50)
    private String factura;

    public DetalleEntrada() {}

    public Integer getIdDetalleEntrada() { return idDetalleEntrada; }
    public void setIdDetalleEntrada(Integer idDetalleEntrada) { this.idDetalleEntrada = idDetalleEntrada; }

    public Entrada getEntrada() { return entrada; }
    public void setEntrada(Entrada entrada) { this.entrada = entrada; }

    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }

    public Integer getProveedorId() { return proveedor != null ? proveedor.getIdProveedor() : null; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getProductoId() { return producto != null ? producto.getIdProducto() : null; }
    public void setProductoId(Integer productoId) {
        if (productoId != null) {
            Producto p = new Producto();
            p.setIdProducto(productoId);
            this.producto = p;
        } else {
            this.producto = null;
        }
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getGuia() { return guia; }
    public void setGuia(String guia) { this.guia = guia; }

    public String getFactura() { return factura; }
    public void setFactura(String factura) { this.factura = factura; }
}
