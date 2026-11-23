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
}
