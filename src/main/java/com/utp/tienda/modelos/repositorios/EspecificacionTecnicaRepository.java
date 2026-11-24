package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.EspecificacionTecnica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspecificacionTecnicaRepository extends JpaRepository<EspecificacionTecnica, Integer> {

    /** Especificaciones activas de un producto, ordenadas por característica. */
    @Query("select e from EspecificacionTecnica e " +
           "where e.producto.idProducto = :idProducto and e.activo = true " +
           "order by e.caracteristica asc")
    List<EspecificacionTecnica> findByIdProductoAndActivoTrue(@Param("idProducto") Integer idProducto);
}
