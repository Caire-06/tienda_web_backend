package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.DespachoLote;
import com.utp.tienda.modelos.entidades.Pedido;
import com.utp.tienda.modelos.entidades.Usuario;
import com.utp.tienda.modelos.repositorios.DespachoLoteRepository;
import com.utp.tienda.modelos.repositorios.PedidoRepository;
import com.utp.tienda.modelos.repositorios.UsuarioRepository;
import com.utp.tienda.vistas.dto.CrearDespachoLoteDTO;
import com.utp.tienda.vistas.dto.DespachoLoteDTO;
import com.utp.tienda.vistas.dto.PedidoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Consolidación y Despacho en Lote (Patrón Composite + State + Observer)
 * Agrupa múltiples pedidos pagados en un manifiesto para entrega conjunta a la empresa de transporte.
 */
@Service
public class DespachoLoteService {

    private final DespachoLoteRepository despachoLoteRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm");

    public DespachoLoteService(DespachoLoteRepository despachoLoteRepository,
                               PedidoRepository pedidoRepository,
                               UsuarioRepository usuarioRepository,
                               PedidoService pedidoService) {
        this.despachoLoteRepository = despachoLoteRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
    }

    @Transactional
    public DespachoLoteDTO crearLote(Integer usuarioId, CrearDespachoLoteDTO dto) {
        if (dto.numerosPedidos() == null || dto.numerosPedidos().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un pedido para el lote de despacho.");
        }

        String username = "ALMACEN";
        if (usuarioId != null) {
            Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
            if (u != null) username = u.getNombreUsuario();
        }

        String codigoLote = generarCodigoLote();
        String guia = dto.numeroGuiaRemision();
        if (guia == null || guia.isBlank()) {
            guia = "GR-T001-" + String.valueOf(System.currentTimeMillis()).substring(7);
        }

        DespachoLote lote = new DespachoLote();
        lote.setCodigoLote(codigoLote);
        lote.setEmpresaTransporte(dto.empresaTransporte().trim());
        lote.setNumeroGuiaRemision(guia.trim());
        lote.setConductor(dto.conductor() != null ? dto.conductor().trim() : "—");
        lote.setPlacaVehiculo(dto.placaVehiculo() != null ? dto.placaVehiculo().trim() : "—");
        lote.setObservaciones(dto.observaciones() != null ? dto.observaciones().trim() : "");
        lote.setFechaDespacho(LocalDateTime.now());
        lote.setFechaCreacion(LocalDateTime.now());
        lote.setFechaActualizacion(LocalDateTime.now());
        lote.setUsuarioRegistro(username);
        lote.setEstado("En_Transito");

        DespachoLote guardado = despachoLoteRepository.save(lote);

        List<String> procesados = new ArrayList<>();
        BigDecimal totalValorizado = BigDecimal.ZERO;

        LocalDateTime ahora = LocalDateTime.now();
        for (String numero : dto.numerosPedidos()) {
            Pedido p = pedidoRepository.findByNumero(numero).orElse(null);
            if (p != null) {
                if ("Por verificar".equalsIgnoreCase(p.getEstadoPago()) || "Pendiente de pago".equalsIgnoreCase(p.getEstado())) {
                    throw new IllegalArgumentException(
                            "El pedido " + p.getNumero() + " tiene pago por transferencia pendiente de confirmación por Administración.");
                }
                p.setDespachoLote(guardado);
                if (p.getFechaProcesando() == null) p.setFechaProcesando(ahora);
                if (p.getFechaEnvio() == null) p.setFechaEnvio(ahora);
                if ("recojo".equalsIgnoreCase(p.getTipoEntrega())) {
                    p.setEstado("Listo para recojo");
                } else {
                    p.setEstado("Enviado");
                }
                pedidoRepository.save(p);
                procesados.add(p.getNumero());
                if (p.getTotal() != null) {
                    totalValorizado = totalValorizado.add(p.getTotal());
                }
            }
        }

        return new DespachoLoteDTO(
                guardado.getIdDespacho(),
                guardado.getCodigoLote(),
                guardado.getEmpresaTransporte(),
                guardado.getNumeroGuiaRemision(),
                guardado.getConductor(),
                guardado.getPlacaVehiculo(),
                guardado.getObservaciones(),
                guardado.getFechaDespacho().format(FECHA_FMT),
                guardado.getUsuarioRegistro(),
                guardado.getEstado(),
                procesados.size(),
                totalValorizado,
                procesados
        );
    }

    @Transactional(readOnly = true)
    public List<DespachoLoteDTO> listarLotes() {
        List<DespachoLote> lotes = despachoLoteRepository.findAllByOrderByFechaDespachoDesc();
        List<DespachoLoteDTO> dtos = new ArrayList<>();

        for (DespachoLote l : lotes) {
            List<Pedido> pedidosDelLote = l.getPedidos();
            List<String> numeros = new ArrayList<>();
            BigDecimal suma = BigDecimal.ZERO;

            if (pedidosDelLote != null) {
                for (Pedido p : pedidosDelLote) {
                    numeros.add(p.getNumero());
                    if (p.getTotal() != null) suma = suma.add(p.getTotal());
                }
            }

            dtos.add(new DespachoLoteDTO(
                    l.getIdDespacho(),
                    l.getCodigoLote(),
                    l.getEmpresaTransporte(),
                    l.getNumeroGuiaRemision(),
                    l.getConductor(),
                    l.getPlacaVehiculo(),
                    l.getObservaciones(),
                    l.getFechaDespacho() != null ? l.getFechaDespacho().format(FECHA_FMT) : "—",
                    l.getUsuarioRegistro(),
                    l.getEstado(),
                    numeros.size(),
                    suma,
                    numeros
            ));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPedidosPendientesDespacho() {
        return pedidoRepository.findAllByOrderByFechaDesc().stream()
                .filter(p -> p.getDespachoLote() == null)
                .filter(p -> !"Cancelado".equalsIgnoreCase(p.getEstado())
                        && !"Entregado".equalsIgnoreCase(p.getEstado())
                        && !"Pendiente de pago".equalsIgnoreCase(p.getEstado())
                        && !"Por verificar".equalsIgnoreCase(p.getEstadoPago()))
                .map(pedidoService::mapear)
                .toList();
    }

    @Transactional
    public DespachoLoteDTO marcarLoteCompletado(Integer id) {
        DespachoLote lote = despachoLoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lote de despacho no encontrado con ID: " + id));

        lote.setEstado("Completado");
        lote.setFechaActualizacion(LocalDateTime.now());
        despachoLoteRepository.save(lote);

        List<Pedido> pedidos = lote.getPedidos();
        List<String> numeros = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;

        LocalDateTime ahora = LocalDateTime.now();
        if (pedidos != null) {
            for (Pedido p : pedidos) {
                if (p.getFechaProcesando() == null) p.setFechaProcesando(ahora);
                if (p.getFechaEnvio() == null) p.setFechaEnvio(ahora);
                if (p.getFechaEntrega() == null) p.setFechaEntrega(ahora);
                p.setEstado("Entregado");
                pedidoRepository.save(p);
                numeros.add(p.getNumero());
                if (p.getTotal() != null) suma = suma.add(p.getTotal());
            }
        }

        return new DespachoLoteDTO(
                lote.getIdDespacho(),
                lote.getCodigoLote(),
                lote.getEmpresaTransporte(),
                lote.getNumeroGuiaRemision(),
                lote.getConductor(),
                lote.getPlacaVehiculo(),
                lote.getObservaciones(),
                lote.getFechaDespacho() != null ? lote.getFechaDespacho().format(FECHA_FMT) : "—",
                lote.getUsuarioRegistro(),
                lote.getEstado(),
                numeros.size(),
                suma,
                numeros
        );
    }

    private String generarCodigoLote() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 1; i <= 99; i++) {
            String codigo = String.format("DESP-%s-%02d", fecha, i);
            if (!despachoLoteRepository.existsByCodigoLote(codigo)) {
                return codigo;
            }
        }
        return "DESP-" + fecha + "-" + (System.currentTimeMillis() % 1000);
    }
}

