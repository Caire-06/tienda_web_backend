package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.InventarioInicial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioInicialRepository extends JpaRepository<InventarioInicial, Integer> {

    Optional<InventarioInicial> findByProducto_IdProducto(Integer productoId);

    List<InventarioInicial> findByActivoTrue();
}
