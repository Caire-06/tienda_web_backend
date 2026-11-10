package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.DespachoLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DespachoLoteRepository extends JpaRepository<DespachoLote, Integer> {
    List<DespachoLote> findAllByOrderByFechaDespachoDesc();
    Optional<DespachoLote> findByCodigoLote(String codigoLote);
    boolean existsByCodigoLote(String codigoLote);
}
