package com.utp.tienda.vistas.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * DTO genérico para respuestas paginadas de la API.
 * Garantiza contratos limpios y consistentes tanto en español como con alias estándar.
 */
public class PaginaDTO<T> {
    private List<T> contenido;
    private long totalElementos;
    private int totalPaginas;
    private int paginaActual; // 0-based index
    private int tamanioPagina;

    public PaginaDTO() {}

    public PaginaDTO(List<T> contenido, long totalElementos, int totalPaginas, int paginaActual, int tamanioPagina) {
        this.contenido = contenido;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
        this.paginaActual = paginaActual;
        this.tamanioPagina = tamanioPagina;
    }

    public static <T> PaginaDTO<T> de(Page<T> page) {
        return new PaginaDTO<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    public List<T> getContenido() {
        return contenido;
    }

    public void setContenido(List<T> contenido) {
        this.contenido = contenido;
    }

    public List<T> getContent() {
        return contenido;
    }

    public long getTotalElementos() {
        return totalElementos;
    }

    public void setTotalElementos(long totalElementos) {
        this.totalElementos = totalElementos;
    }

    public long getTotalElements() {
        return totalElementos;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public int getTotalPages() {
        return totalPaginas;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    public int getNumber() {
        return paginaActual;
    }

    public int getTamanioPagina() {
        return tamanioPagina;
    }

    public void setTamanioPagina(int tamanioPagina) {
        this.tamanioPagina = tamanioPagina;
    }

    public int getSize() {
        return tamanioPagina;
    }
}
