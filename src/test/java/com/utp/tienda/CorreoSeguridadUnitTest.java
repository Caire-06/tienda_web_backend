package com.utp.tienda;

import com.utp.tienda.controladores.filtros.AutorizacionDecoratorFilter;
import com.utp.tienda.controladores.filtros.AutorizacionDecoratorFilter.PoliticaRuta;
import com.utp.tienda.modelos.servicios.ControlTraficoService;
import com.utp.tienda.modelos.servicios.SesionService;
import com.utp.tienda.patrones.adapter.BrevoEmailAdapter;
import com.utp.tienda.patrones.template.PlantillaCorreoBase;
import com.utp.tienda.patrones.template.PlantillaCorreoBase.ContextoCorreo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorreoSeguridadUnitTest {

    @Test
    void plantillasRenderizanEscapandoHtmlYOtp() {
        PlantillaCorreoBase registro = PlantillaCorreoBase.paraProposito("REGISTRO");
        ContextoCorreo ctx = new ContextoCorreo("<Gustavo>", "482910", 15, null, null);
        String html = registro.construirHtml(ctx);

        assertTrue(registro.construirAsunto(ctx).contains("Verifica"));
        assertTrue(html.contains("482910"));
        assertTrue(html.contains("&lt;Gustavo&gt;"));
        assertFalse(html.contains("<Gustavo>"));

        PlantillaCorreoBase twoFa = PlantillaCorreoBase.paraProposito("LOGIN_2FA");
        assertTrue(twoFa.construirAsunto(ctx).contains("2FA"));

        PlantillaCorreoBase recuperar = PlantillaCorreoBase.paraProposito("RECUPERAR_PASSWORD");
        assertTrue(recuperar.construirAsunto(ctx).contains("contraseña"));

        PlantillaCorreoBase bienvenida = PlantillaCorreoBase.paraProposito("BIENVENIDA_ADMIN");
        ContextoCorreo ctxAdmin = new ContextoCorreo("Ana Perez", null, null, "aperez", "Temp123!");
        String htmlAdmin = bienvenida.construirHtml(ctxAdmin);
        assertTrue(htmlAdmin.contains("aperez"));
        assertTrue(htmlAdmin.contains("Temp123!"));
    }

    @Test
    void brevoAdapterValidaCredencialesObligatorias() {
        BrevoEmailAdapter sinConfig = new BrevoEmailAdapter("", "", "TechStore");
        assertFalse(sinConfig.estaConfigurado());

        BrevoEmailAdapter sinRemitente = new BrevoEmailAdapter("xkeysib-demo", "   ", "TechStore");
        assertFalse(sinRemitente.estaConfigurado());

        BrevoEmailAdapter configurado = new BrevoEmailAdapter("xkeysib-demo", "noreply@techstore.com", "TechStore");
        assertTrue(configurado.estaConfigurado());
    }

    @Test
    void sesionServiceRevocaTodasLasSesionesDelUsuarioTrasCambioDeClave() {
        SesionService sesionService = new SesionService();
        String tk1 = sesionService.crear(99);
        String tk2 = sesionService.crear(99);
        String tkOtro = sesionService.crear(100);

        assertEquals(99, sesionService.identificar("Bearer " + tk1));
        assertEquals(99, sesionService.identificar("Bearer " + tk2));

        sesionService.revocarPorUsuario(99);

        assertNull(sesionService.identificar("Bearer " + tk1));
        assertNull(sesionService.identificar("Bearer " + tk2));
        assertEquals(100, sesionService.identificar("Bearer " + tkOtro));
    }

    @Test
    void controlTraficoBloqueaExcesoDeTasaYCuotaDiariaBrevo() {
        ControlTraficoService control = new ControlTraficoService(2, 2, 2);

        assertTrue(control.intentarConsumir("auth-registro", "190.10.20.30", 2, 60).permitido());
        assertTrue(control.intentarConsumir("auth-registro", "190.10.20.30", 2, 60).permitido());
        ControlTraficoService.ResultadoLimite bloqueado =
                control.intentarConsumir("auth-registro", "190.10.20.30", 2, 60);
        assertFalse(bloqueado.permitido());
        assertTrue(bloqueado.segundosEspera() >= 1);

        control.consumirCuotaDiariaBrevo();
        control.consumirCuotaDiariaBrevo();
        assertThrows(ControlTraficoService.LimiteCuotaDiariaException.class,
                control::consumirCuotaDiariaBrevo);
    }

    @Test
    void autorizacionRbacProtegeRutasAdminYAlmacenPorDefecto() {
        assertEquals(PoliticaRuta.PUBLICO, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/productos"));
        assertEquals(PoliticaRuta.PUBLICO, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/productos/15"));
        assertEquals(PoliticaRuta.PUBLICO, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/consultas/dni/12345678"));
        assertEquals(PoliticaRuta.PUBLICO, AutorizacionDecoratorFilter.resolverPolitica("POST", "/api/auth/login"));

        assertEquals(PoliticaRuta.SOLO_ADMIN, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/usuarios"));
        assertEquals(PoliticaRuta.SOLO_ADMIN, AutorizacionDecoratorFilter.resolverPolitica("POST", "/api/usuarios"));
        assertEquals(PoliticaRuta.SOLO_ADMIN, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/admin/dashboard/kpis"));
        assertEquals(PoliticaRuta.SOLO_ADMIN, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/admin/reportes/ventas"));
        assertEquals(PoliticaRuta.SOLO_ADMIN, AutorizacionDecoratorFilter.resolverPolitica("PUT", "/api/pedidos/PED-001/pago"));

        assertEquals(PoliticaRuta.ADMIN_O_ALMACEN, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/despachos"));
        assertEquals(PoliticaRuta.ADMIN_O_ALMACEN, AutorizacionDecoratorFilter.resolverPolitica("PATCH", "/api/despachos/5/completar"));
        assertEquals(PoliticaRuta.ADMIN_O_ALMACEN, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/pedidos"));
        assertEquals(PoliticaRuta.ADMIN_O_ALMACEN, AutorizacionDecoratorFilter.resolverPolitica("DELETE", "/api/productos/1"));
        assertEquals(PoliticaRuta.ADMIN_O_ALMACEN, AutorizacionDecoratorFilter.resolverPolitica("POST", "/api/entradas"));
        assertEquals(PoliticaRuta.ADMIN_O_ALMACEN, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/kardex/movimientos"));

        assertEquals(PoliticaRuta.AUTENTICADO, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/perfil"));
        assertEquals(PoliticaRuta.AUTENTICADO, AutorizacionDecoratorFilter.resolverPolitica("POST", "/api/pedidos"));
        assertEquals(PoliticaRuta.BLOQUEADO, AutorizacionDecoratorFilter.resolverPolitica("GET", "/api/admin/../productos"));

        assertFalse(AutorizacionDecoratorFilter.rolPermitido(PoliticaRuta.SOLO_ADMIN, "CLIENTE"));
        assertFalse(AutorizacionDecoratorFilter.rolPermitido(PoliticaRuta.SOLO_ADMIN, "ALMACEN"));
        assertTrue(AutorizacionDecoratorFilter.rolPermitido(PoliticaRuta.SOLO_ADMIN, "ADMINISTRADOR"));
        assertTrue(AutorizacionDecoratorFilter.rolPermitido(PoliticaRuta.ADMIN_O_ALMACEN, "ALMACEN"));
        assertFalse(AutorizacionDecoratorFilter.rolPermitido(PoliticaRuta.ADMIN_O_ALMACEN, "CLIENTE"));
    }
}

