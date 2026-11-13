package com.utp.tienda.controladores;

import com.utp.tienda.modelos.servicios.ConsultaFiscalService;
import com.utp.tienda.modelos.servicios.ControlTraficoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaFiscalController {

    private final ConsultaFiscalService consultaFiscalService;

    public ConsultaFiscalController(ConsultaFiscalService consultaFiscalService) {
        this.consultaFiscalService = consultaFiscalService;
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<?> consultarDni(@PathVariable String dni) {
        return ejecutarConsulta(() -> consultaFiscalService.consultarDni(dni));
    }

    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<?> consultarRuc(@PathVariable String ruc) {
        return ejecutarConsulta(() -> consultaFiscalService.consultarRuc(ruc));
    }

    private ResponseEntity<?> ejecutarConsulta(java.util.function.Supplier<Object> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ConsultaFiscalService.DocumentoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (ConsultaFiscalService.TokenNoConfiguradoException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("sinToken", true, "error", e.getMessage()));
        } catch (ControlTraficoService.LimiteCuotaDiariaException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }
}
