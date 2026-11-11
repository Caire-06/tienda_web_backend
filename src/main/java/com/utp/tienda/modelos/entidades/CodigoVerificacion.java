package com.utp.tienda.modelos.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad normalizada (3FN) para códigos OTP de verificación de correo,
 * autenticación en dos pasos (2FA) y recuperación de contraseña.
 */
@Entity
@Table(name = "codigos_verificacion")
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo")
    private Integer idCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "proposito", nullable = false, length = 30)
    private String proposito;

    @Column(name = "correo_destino", nullable = false, length = 100)
    private String correoDestino;

    @Column(name = "desafio_token", nullable = false, unique = true, length = 80)
    private String desafioToken;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "max_intentos", nullable = false)
    private Integer maxIntentos = 5;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_ultimo_reenvio")
    private LocalDateTime fechaUltimoReenvio;

    @Column(name = "usado", nullable = false)
    private Boolean usado = false;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    // Columnas de auditoría obligatorias
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "usuario_registro", length = 50)
    private String usuarioRegistro = "SISTEMA";

    public CodigoVerificacion() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = ahora;
        }
        this.fechaActualizacion = ahora;
        if (this.fechaUltimoReenvio == null) {
            this.fechaUltimoReenvio = ahora;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdCodigo() { return idCodigo; }
    public void setIdCodigo(Integer idCodigo) { this.idCodigo = idCodigo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getProposito() { return proposito; }
    public void setProposito(String proposito) { this.proposito = proposito; }

    public String getCorreoDestino() { return correoDestino; }
    public void setCorreoDestino(String correoDestino) { this.correoDestino = correoDestino; }

    public String getDesafioToken() { return desafioToken; }
    public void setDesafioToken(String desafioToken) { this.desafioToken = desafioToken; }

    public String getCodigoHash() { return codigoHash; }
    public void setCodigoHash(String codigoHash) { this.codigoHash = codigoHash; }

    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public Integer getMaxIntentos() { return maxIntentos; }
    public void setMaxIntentos(Integer maxIntentos) { this.maxIntentos = maxIntentos; }

    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public LocalDateTime getFechaUltimoReenvio() { return fechaUltimoReenvio; }
    public void setFechaUltimoReenvio(LocalDateTime fechaUltimoReenvio) { this.fechaUltimoReenvio = fechaUltimoReenvio; }

    public Boolean getUsado() { return Boolean.TRUE.equals(usado); }
    public void setUsado(Boolean usado) { this.usado = usado; }

    public LocalDateTime getFechaUso() { return fechaUso; }
    public void setFechaUso(LocalDateTime fechaUso) { this.fechaUso = fechaUso; }

    public Boolean getEstado() { return Boolean.TRUE.equals(estado); }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}

