package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "tipo_entrega", length = 20)
    private String tipoEntrega;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(name = "correo_contacto", length = 120)
    private String correoContacto;

    @Column(name = "celular_contacto", length = 20)
    private String celularContacto;

    @Column(name = "direccion_envio", length = 200)
    private String direccionEnvio;

    @Column(name = "referencia_envio", length = 200)
    private String referenciaEnvio;

    @Column(name = "distrito_envio", length = 100)
    private String distritoEnvio;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ComprobantePedido comprobante;

    public Pedido() {}

    public Integer getIdPedido() { return idPedido; }
    public void setIdPedido(Integer idPedido) { this.idPedido = idPedido; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getCorreoContacto() { return correoContacto; }
    public void setCorreoContacto(String correoContacto) { this.correoContacto = correoContacto; }

    public String getCelularContacto() { return celularContacto; }
    public void setCelularContacto(String celularContacto) { this.celularContacto = celularContacto; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }

    public String getReferenciaEnvio() { return referenciaEnvio; }
    public void setReferenciaEnvio(String referenciaEnvio) { this.referenciaEnvio = referenciaEnvio; }

    public String getDistritoEnvio() { return distritoEnvio; }
    public void setDistritoEnvio(String distritoEnvio) { this.distritoEnvio = distritoEnvio; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }

    public ComprobantePedido getComprobante() { return comprobante; }
    public void setComprobante(ComprobantePedido comprobante) { this.comprobante = comprobante; }
}
