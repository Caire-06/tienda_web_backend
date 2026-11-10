package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la consolidación y despacho en lote de pedidos (Etapa 6 - Logística).
 * Patrón Composite: Agrupa múltiples pedidos para ser entregados en un solo manifiesto al Courier.
 * Cumple con 3FN y las columnas obligatorias de auditoría.
 */
@Entity
@Table(name = "despachos_lotes")
public class DespachoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despacho")
    private Integer idDespacho;

    @Column(name = "codigo_lote", nullable = false, unique = true, length = 30)
    private String codigoLote;

    @Column(name = "empresa_transporte", nullable = false, length = 80)
    private String empresaTransporte;

    @Column(name = "numero_guia_remision", length = 40)
    private String numeroGuiaRemision;

    @Column(name = "conductor", length = 100)
    private String conductor;

    @Column(name = "placa_vehiculo", length = 20)
    private String placaVehiculo;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @Column(name = "fecha_despacho", nullable = false)
    private LocalDateTime fechaDespacho = LocalDateTime.now();

    // Columnas de auditoría estándar obligatorias
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Column(name = "usuario_registro", nullable = false, length = 50)
    private String usuarioRegistro = "SISTEMA";

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "En_Transito";

    @OneToMany(mappedBy = "despachoLote", fetch = FetchType.LAZY)
    private List<Pedido> pedidos = new ArrayList<>();

    public DespachoLote() {}

    public Integer getIdDespacho() { return idDespacho; }
    public void setIdDespacho(Integer idDespacho) { this.idDespacho = idDespacho; }

    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }

    public String getEmpresaTransporte() { return empresaTransporte; }
    public void setEmpresaTransporte(String empresaTransporte) { this.empresaTransporte = empresaTransporte; }

    public String getNumeroGuiaRemision() { return numeroGuiaRemision; }
    public void setNumeroGuiaRemision(String numeroGuiaRemision) { this.numeroGuiaRemision = numeroGuiaRemision; }

    public String getConductor() { return conductor; }
    public void setConductor(String conductor) { this.conductor = conductor; }

    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDateTime fechaDespacho) { this.fechaDespacho = fechaDespacho; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<Pedido> getPedidos() { return pedidos; }
    public void setPedidos(List<Pedido> pedidos) { this.pedidos = pedidos; }
}

