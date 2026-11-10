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

    @Column(name = "tipo_entrega", length = 30)
    private String tipoEntrega;

    @Column(name = "costo_envio", precision = 12, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Column(name = "fecha_procesando")
    private LocalDateTime fechaProcesando;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(name = "estado_pago", length = 30)
    private String estadoPago = "Pagado";

    @Column(name = "codigo_operacion_pago", length = 60)
    private String codigoOperacionPago;

    @Column(name = "comprobante_pago_url", length = 500)
    private String comprobantePagoUrl;

    @Column(name = "fecha_confirmacion_pago")
    private LocalDateTime fechaConfirmacionPago;

    @Column(name = "usuario_confirmo_pago", length = 60)
    private String usuarioConfirmoPago;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_despacho_lote")
    private DespachoLote despachoLote;

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

    public DespachoLote getDespachoLote() { return despachoLote; }
    public void setDespachoLote(DespachoLote despachoLote) { this.despachoLote = despachoLote; }

    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(BigDecimal costoEnvio) { this.costoEnvio = costoEnvio; }

    public LocalDateTime getFechaProcesando() { return fechaProcesando; }
    public void setFechaProcesando(LocalDateTime fechaProcesando) { this.fechaProcesando = fechaProcesando; }

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getCodigoOperacionPago() { return codigoOperacionPago; }
    public void setCodigoOperacionPago(String codigoOperacionPago) { this.codigoOperacionPago = codigoOperacionPago; }

    public String getComprobantePagoUrl() { return comprobantePagoUrl; }
    public void setComprobantePagoUrl(String comprobantePagoUrl) { this.comprobantePagoUrl = comprobantePagoUrl; }

    public LocalDateTime getFechaConfirmacionPago() { return fechaConfirmacionPago; }
    public void setFechaConfirmacionPago(LocalDateTime fechaConfirmacionPago) { this.fechaConfirmacionPago = fechaConfirmacionPago; }

    public String getUsuarioConfirmoPago() { return usuarioConfirmoPago; }
    public void setUsuarioConfirmoPago(String usuarioConfirmoPago) { this.usuarioConfirmoPago = usuarioConfirmoPago; }
}
