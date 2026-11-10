package com.utp.tienda.modelos.repositorios;

import com.utp.tienda.modelos.entidades.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntradaRepository extends JpaRepository<Entrada, Integer> {
    List<Entrada> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario);
}
