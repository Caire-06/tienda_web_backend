package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findAllByOrderByNombreAsc();
    Optional<Producto> findByNombre(String nombre);
    boolean existsByNombre(String nombre);

    /** Busca un producto aplicando bloqueo pesimista (PESSIMISTIC_WRITE) para operaciones concurrentes seguras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto = :id")
    Optional<Producto> findByIdConBloqueo(@Param("id") Integer id);
}
