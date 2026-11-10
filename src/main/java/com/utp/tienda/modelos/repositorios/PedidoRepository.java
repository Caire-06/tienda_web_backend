package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findByNumero(String numero);
    List<Pedido> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario);
    List<Pedido> findAllByOrderByFechaDesc();
    boolean existsByNumero(String numero);
}
