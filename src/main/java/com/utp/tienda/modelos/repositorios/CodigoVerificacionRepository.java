package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Integer> {

    Optional<CodigoVerificacion> findByDesafioTokenAndEstadoTrue(String desafioToken);

    Optional<CodigoVerificacion> findTopByUsuarioIdUsuarioAndPropositoAndUsadoFalseAndEstadoTrueOrderByFechaCreacionDesc(
            Integer idUsuario, String proposito);

    @Modifying
    @Query("UPDATE CodigoVerificacion c SET c.estado = false " +
           "WHERE c.usuario.idUsuario = :idUsuario AND c.proposito = :proposito " +
           "AND c.usado = false AND c.estado = true")
    int invalidarActivosPorUsuarioYProposito(@Param("idUsuario") Integer idUsuario,
                                             @Param("proposito") String proposito);
}

