package com.utp.tienda.patrones.template;

/**
 * Patrón GoF Template Method para generar correos transaccionales HTML de TechStore
 * con estructura uniforme, segura (escape HTML) y responsiva en clientes de correo.
 */
public abstract class PlantillaCorreoBase {

    public record ContextoCorreo(
            String nombreDestinatario,
            String codigoOtp,
            Integer minutosExpiracion,
            String nombreUsuario,
            String passwordTemporal
    ) {}

    public final String construirAsunto(ContextoCorreo ctx) {
        return definirAsunto(ctx);
    }

    public final String construirHtml(ContextoCorreo ctx) {
        String nombreSeguro = escaparHtml(
                ctx.nombreDestinatario() != null && !ctx.nombreDestinatario().isBlank()
                        ? ctx.nombreDestinatario().trim()
                        : "cliente"
        );
        String titulo = escaparHtml(definirTitulo(ctx));
        String cuerpo = construirCuerpoHtml(ctx);

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background-color:#0f172a;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#e2e8f0;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#0f172a;padding:28px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#111827;border:1px solid #1e293b;border-radius:16px;overflow:hidden;">
                          <tr>
                            <td style="padding:24px 28px;background:linear-gradient(135deg,#1d4ed8,#0ea5e9);color:#ffffff;">
                              <div style="font-size:20px;font-weight:700;letter-spacing:-0.02em;">TechStore</div>
                              <div style="font-size:13px;opacity:0.9;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;font-size:15px;line-height:1.6;color:#cbd5e1;">
                              <p style="margin:0 0 16px 0;color:#f8fafc;">Hola <strong>%s</strong>,</p>
                              %s
                              <hr style="border:none;border-top:1px solid #1e293b;margin:24px 0 16px 0;" />
                              <p style="margin:0;font-size:12px;color:#64748b;">
                                Si no solicitaste esta operación, puedes ignorar este mensaje o cambiar tu contraseña desde Mi Cuenta.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(titulo, nombreSeguro, cuerpo);
    }

    protected abstract String definirAsunto(ContextoCorreo ctx);

    protected abstract String definirTitulo(ContextoCorreo ctx);

    protected abstract String construirCuerpoHtml(ContextoCorreo ctx);

    protected final String renderizarCajaOtp(String codigo, int minutos) {
        String codigoSeguro = escaparHtml(codigo != null ? codigo : "");
        return """
                <div style="margin:22px 0;padding:18px;background-color:#0b1220;border:1px dashed #38bdf8;border-radius:12px;text-align:center;">
                  <div style="font-size:12px;text-transform:uppercase;letter-spacing:0.12em;color:#94a3b8;margin-bottom:8px;">
                    Código de verificación
                  </div>
                  <div style="font-size:32px;font-weight:800;letter-spacing:0.28em;color:#38bdf8;font-family:Consolas,monospace;">
                    %s
                  </div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:10px;">
                    Válido por <strong>%d minutos</strong> y de un solo uso. No compartas este código.
                  </div>
                </div>
                """.formatted(codigoSeguro, minutos);
    }

    protected static String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static PlantillaCorreoBase paraProposito(String proposito) {
        if ("LOGIN_2FA".equalsIgnoreCase(proposito)) {
            return new CodigoTwoFactor();
        }
        if ("RECUPERAR_PASSWORD".equalsIgnoreCase(proposito)) {
            return new RecuperacionPassword();
        }
        if ("BIENVENIDA_ADMIN".equalsIgnoreCase(proposito)) {
            return new BienvenidaAdmin();
        }
        return new VerificacionRegistro();
    }

    public static final class VerificacionRegistro extends PlantillaCorreoBase {
        @Override
        protected String definirAsunto(ContextoCorreo ctx) {
            return "Verifica tu correo en TechStore";
        }

        @Override
        protected String definirTitulo(ContextoCorreo ctx) {
            return "Activación de cuenta";
        }

        @Override
        protected String construirCuerpoHtml(ContextoCorreo ctx) {
            int mins = ctx.minutosExpiracion() != null ? ctx.minutosExpiracion() : 15;
            return "<p style=\"margin:0 0 12px 0;\">Gracias por registrarte en <strong>TechStore</strong>. "
                    + "Ingresa el siguiente código de 6 dígitos para confirmar tu correo electrónico y activar tu acceso:</p>"
                    + renderizarCajaOtp(ctx.codigoOtp(), mins);
        }
    }

    public static final class CodigoTwoFactor extends PlantillaCorreoBase {
        @Override
        protected String definirAsunto(ContextoCorreo ctx) {
            return "Código de seguridad (2FA) - TechStore";
        }

        @Override
        protected String definirTitulo(ContextoCorreo ctx) {
            return "Verificación en dos pasos";
        }

        @Override
        protected String construirCuerpoHtml(ContextoCorreo ctx) {
            int mins = ctx.minutosExpiracion() != null ? ctx.minutosExpiracion() : 10;
            return "<p style=\"margin:0 0 12px 0;\">Detectamos un intento de inicio de sesión en tu cuenta de <strong>TechStore</strong>. "
                    + "Para completar el acceso, ingresa este código de verificación:</p>"
                    + renderizarCajaOtp(ctx.codigoOtp(), mins);
        }
    }

    public static final class RecuperacionPassword extends PlantillaCorreoBase {
        @Override
        protected String definirAsunto(ContextoCorreo ctx) {
            return "Restablece tu contraseña - TechStore";
        }

        @Override
        protected String definirTitulo(ContextoCorreo ctx) {
            return "Recuperación de contraseña";
        }

        @Override
        protected String construirCuerpoHtml(ContextoCorreo ctx) {
            int mins = ctx.minutosExpiracion() != null ? ctx.minutosExpiracion() : 15;
            return "<p style=\"margin:0 0 12px 0;\">Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>TechStore</strong>. "
                    + "Utiliza el siguiente código junto con tu nueva clave:</p>"
                    + renderizarCajaOtp(ctx.codigoOtp(), mins);
        }
    }

    public static final class BienvenidaAdmin extends PlantillaCorreoBase {
        @Override
        protected String definirAsunto(ContextoCorreo ctx) {
            return "Tu cuenta en TechStore ha sido creada";
        }

        @Override
        protected String definirTitulo(ContextoCorreo ctx) {
            return "Credenciales de acceso";
        }

        @Override
        protected String construirCuerpoHtml(ContextoCorreo ctx) {
            String user = escaparHtml(ctx.nombreUsuario() != null ? ctx.nombreUsuario() : "");
            String tempPass = escaparHtml(ctx.passwordTemporal() != null ? ctx.passwordTemporal() : "");
            return """
                    <p style="margin:0 0 12px 0;">Un administrador ha habilitado tu cuenta en <strong>TechStore</strong>.</p>
                    <div style="margin:18px 0;padding:16px;background-color:#0b1220;border:1px solid #334155;border-radius:12px;">
                      <p style="margin:0 0 8px 0;"><strong>Usuario:</strong> <span style="color:#38bdf8;">%s</span></p>
                      <p style="margin:0;"><strong>Contraseña temporal:</strong> <code style="color:#fbbf24;font-size:15px;">%s</code></p>
                    </div>
                    <p style="margin:0;">Al ingresar por primera vez se te solicitará definir una nueva contraseña personal.</p>
                    """.formatted(user, tempPass);
        }
    }
}

