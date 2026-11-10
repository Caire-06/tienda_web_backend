package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.ComprobantePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComprobantePedidoRepository extends JpaRepository<ComprobantePedido, Integer> {
    Optional<ComprobantePedido> findFirstByDocumentoOrderByIdComprobanteDesc(String documento);
}
