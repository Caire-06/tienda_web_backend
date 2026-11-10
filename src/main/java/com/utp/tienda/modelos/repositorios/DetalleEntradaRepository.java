package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.DetalleEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleEntradaRepository extends JpaRepository<DetalleEntrada, Integer> {
}
