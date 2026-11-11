package com.utp.tienda;

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
}

