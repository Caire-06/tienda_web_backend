package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findByNumero(String numero);
    List<Pedido> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario);
    Page<Pedido> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario, Pageable pageable);
    List<Pedido> findAllByOrderByFechaDesc();
    Page<Pedido> findAllByOrderByFechaDesc(Pageable pageable);
    boolean existsByNumero(String numero);

    @Query("SELECT p FROM Pedido p WHERE p.despachoLote IS NULL AND LOWER(p.estado) NOT IN ('cancelado', 'entregado', 'pendiente de pago') AND LOWER(COALESCE(p.estadoPago, '')) <> 'por verificar' ORDER BY p.fecha DESC")
    Page<Pedido> findPedidosPendientesDespachoPaginado(Pageable pageable);
}
