package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.Proveedor;
import com.utp.tienda.modelos.repositorios.ProveedorRepository;
import com.utp.tienda.vistas.dto.CrearProveedorDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProveedorService {
    private final ProveedorRepository proveedores;
    private final com.utp.tienda.modelos.servicios.ProductoService productoService;

    public ProveedorService(ProveedorRepository proveedores, com.utp.tienda.modelos.servicios.ProductoService productoService) {
        this.proveedores = proveedores;
        this.productoService = productoService;
    }

    /** Lista los proveedores activos (catálogo para seleccionar al registrar entradas). */
    public List<Proveedor> listarActivos() {
        return proveedores.findByActivoTrueOrderByNombreAsc().stream().toList();
    }

    public org.springframework.data.domain.Page<Proveedor> listarActivosPaginado(org.springframework.data.domain.Pageable pageable) {
        return proveedores.findByActivoTrueOrderByNombreAsc(pageable);
    }

    /** Registra un nuevo proveedor (operación restringida a roles administrativos/almacén). */
    public Proveedor crear(Integer usuarioId, CrearProveedorDTO dto) {
        productoService.verificarRolPermitido(usuarioId);
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.nombre());
        proveedor.setRuc(dto.ruc());
        proveedor.setContacto(dto.contacto());
        proveedor.setTelefono(dto.telefono());
        proveedor.setEmail(dto.email());
        proveedor.setDireccion(dto.direccion());
        proveedor.setActivo(dto.activo() != null ? dto.activo() : true);
        return proveedores.save(proveedor);
    }

    /** Actualiza un proveedor existente con auditoría. */
    public Proveedor actualizar(Integer usuarioId, Integer idProveedor, CrearProveedorDTO dto) {
        productoService.verificarRolPermitido(usuarioId);
        Proveedor proveedor = proveedores.findById(idProveedor)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Proveedor no encontrado con ID: " + idProveedor));
        proveedor.setNombre(dto.nombre());
        proveedor.setRuc(dto.ruc());
        proveedor.setContacto(dto.contacto());
        proveedor.setTelefono(dto.telefono());
        proveedor.setEmail(dto.email());
        proveedor.setDireccion(dto.direccion());
        if (dto.activo() != null) {
            proveedor.setActivo(dto.activo());
        }
        proveedor.setFechaActualizacion(java.time.LocalDateTime.now());
        return proveedores.save(proveedor);
    }
}
