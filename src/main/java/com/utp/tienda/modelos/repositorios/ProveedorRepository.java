package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    List<Proveedor> findByActivoTrueOrderByNombreAsc();
    Page<Proveedor> findByActivoTrueOrderByNombreAsc(Pageable pageable);
}
