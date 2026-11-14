package com.utp.tienda.modelos.servicios;

import com.utp.tienda.modelos.entidades.*;
import com.utp.tienda.modelos.repositorios.*;
import com.utp.tienda.vistas.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final DatosPersonalesRepository datosRepo;
    private final CorreoElectronicoRepository correos;
    private final TelefonoRepository telefonos;
    private final DireccionRepository direcciones;
    private final DistritoRepository distritos;
    private final SexoRepository sexos;
    private final EstadoCivilRepository estadosCiviles;
    private final PasswordEncryptionStrategy encryptionStrategy;

    public PerfilService(UsuarioRepository usuarios, DatosPersonalesRepository datosRepo,
                         CorreoElectronicoRepository correos, TelefonoRepository telefonos,
                         DireccionRepository direcciones, DistritoRepository distritos,
                         SexoRepository sexos, EstadoCivilRepository estadosCiviles,
                         @Qualifier("sha256Strategy") PasswordEncryptionStrategy encryptionStrategy) {
        this.usuarios = usuarios;
        this.datosRepo = datosRepo;
        this.correos = correos;
        this.telefonos = telefonos;
        this.direcciones = direcciones;
        this.distritos = distritos;
        this.sexos = sexos;
        this.estadosCiviles = estadosCiviles;
        this.encryptionStrategy = encryptionStrategy;
    }

    private Distrito resolverDistrito(String nombreDistrito) {
        if (nombreDistrito != null && !nombreDistrito.isBlank()) {
            String buscado = nombreDistrito.trim();
            Distrito exacto = distritos.findByNombreDistrito(buscado).orElse(null);
            if (exacto != null) return exacto;
            for (Distrito d : distritos.findAll()) {
                if (d.getNombreDistrito() != null && d.getNombreDistrito().equalsIgnoreCase(buscado)) {
                    return d;
                }
            }
        }
        return distritos.findByNombreDistrito("San Juan de Lurigancho")
                .orElseGet(() -> distritos.findAll().stream().findFirst().orElse(null));
    }

    @Transactional(readOnly = true)
    public PerfilDTO consultar(Integer idUsuario) {
        Usuario usuario = usuarios.findById(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        DatosPersonales datos = datosRepo.findByUsuario_IdUsuario(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene datos personales"));
        Integer id = datos.getIdDatosPersonales();
        List<CorreoElectronico> listaCorreos = correos.findByDatosPersonales_IdDatosPersonales(id);
        List<Telefono> listaTelefonos = telefonos.findByDatosPersonales_IdDatosPersonales(id);
        List<Direccion> listaDirecciones = direcciones.findByDatosPersonales_IdDatosPersonales(id);
        String correo = listaCorreos.isEmpty() ? "" : listaCorreos.get(0).getCorreoElectronico();
        String telefono = listaTelefonos.isEmpty() ? "" : listaTelefonos.get(0).getNumeroTelefono();
        Direccion dir = listaDirecciones.isEmpty() ? null : listaDirecciones.get(0);
        return new PerfilDTO(idUsuario, datos.getNombres(), datos.getApellidos(), datos.getDni(),
            correo, telefono, dir == null ? "" : dir.getCalleAvenida(),
            dir == null || dir.getDistrito() == null ? "" : dir.getDistrito().getNombreDistrito(),
            usuario.getRol() == null ? "" : usuario.getRol().getNombreRol(),
            Boolean.TRUE.equals(usuario.getRequiereCambioPassword()));
    }

    @Transactional
    public PerfilDTO actualizar(Integer idUsuario, ActualizarPerfilDTO request) {
        DatosPersonales datos = datosRepo.findByUsuario_IdUsuario(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene datos personales"));
        String documento = request.documento().trim();
        datosRepo.findByDni(documento).ifPresent(otro -> {
            if (!otro.getIdDatosPersonales().equals(datos.getIdDatosPersonales()))
                throw new IllegalArgumentException("El documento pertenece a otro usuario");
        });
        datos.setNombres(request.nombres().trim());
        datos.setApellidos(request.apellidos().trim());
        datos.setDni(documento);
        datosRepo.save(datos);

        Integer id = datos.getIdDatosPersonales();
        if (request.telefono() != null) {
            List<Telefono> lista = telefonos.findByDatosPersonales_IdDatosPersonales(id);
            String numero = request.telefono().trim();
            if (numero.isEmpty()) {
                if (!lista.isEmpty()) telefonos.delete(lista.get(0));
            } else {
                Telefono telefono = lista.isEmpty() ? new Telefono() : lista.get(0);
                telefono.setDatosPersonales(datos);
                telefono.setNumeroTelefono(numero);
                if (lista.isEmpty()) telefono.setTipoTelefono("MOVIL");
                telefonos.save(telefono);
            }
        }
        if (request.direccion() != null) {
            List<Direccion> lista = direcciones.findByDatosPersonales_IdDatosPersonales(id);
            String calle = request.direccion().trim();
            if (calle.isEmpty()) {
                if (!lista.isEmpty()) direcciones.delete(lista.get(0));
            } else {
                Direccion direccion = lista.isEmpty() ? new Direccion() : lista.get(0);
                direccion.setDatosPersonales(datos);
                direccion.setCalleAvenida(calle);
                if (lista.isEmpty()) {
                    direccion.setTipoDireccion("DOMICILIO");
                    direccion.setNumero("S/N");
                }
                if (request.idDistrito() != null) {
                    direccion.setDistrito(distritos.findById(request.idDistrito())
                        .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado")));
                }
                if (direccion.getDistrito() == null) {
                    direccion.setDistrito(resolverDistrito(null));
                }
                if (direccion.getDistrito() == null) {
                    throw new IllegalArgumentException("No hay distritos disponibles para registrar la dirección");
                }
                direcciones.save(direccion);
            }
        }
        return consultar(idUsuario);
    }

    /**
     * Sincroniza en las tablas 3FN (DatosPersonales, Telefono, Direccion, CorreoElectronico)
     * los datos ingresados o completados por el usuario durante la compra en el carrito.
     */
    @Transactional
    public void sincronizarDesdeCompra(Usuario usuario, CrearPedidoDTO dto) {
        if (usuario == null || dto == null) return;

        DatosPersonales datos = datosRepo.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElse(null);
        if (datos == null) {
            Sexo sexoDefault = sexos.findByDescripcion("No Especifica")
                    .orElseGet(() -> sexos.save(new Sexo(null, "No Especifica")));
            EstadoCivil estadoCivilDefault = estadosCiviles.findByDescripcion("Soltero(a)")
                    .orElseGet(() -> estadosCiviles.save(new EstadoCivil(null, "Soltero(a)")));
            datos = new DatosPersonales();
            datos.setUsuario(usuario);
            datos.setNombres(usuario.getNombreUsuario());
            datos.setApellidos("");
            datos.setDni("CLI" + String.format("%08d", usuario.getIdUsuario()));
            datos.setSexo(sexoDefault);
            datos.setEstadoCivil(estadoCivilDefault);
            datos = datosRepo.save(datos);
        }

        boolean cambioDatos = false;
        if (dto.comprobante() != null && "Boleta".equalsIgnoreCase(dto.comprobante().tipo())) {
            String dniCompra = dto.comprobante().documento() != null ? dto.comprobante().documento().trim() : "";
            String dniActual = datos.getDni() != null ? datos.getDni().trim() : "";
            boolean actualEsSintetico = dniActual.isEmpty() || dniActual.startsWith("CLI") || dniActual.startsWith("DNI");
            if (dniCompra.matches("\\d{8}") && actualEsSintetico) {
                Integer idDatosActual = datos.getIdDatosPersonales();
                boolean ocupado = datosRepo.findByDni(dniCompra)
                        .filter(otro -> !otro.getIdDatosPersonales().equals(idDatosActual))
                        .isPresent();
                if (!ocupado) {
                    datos.setDni(dniCompra);
                    cambioDatos = true;
                }
            }
            String nomCompra = dto.comprobante().nombreRazonSocial() != null ? dto.comprobante().nombreRazonSocial().trim() : "";
            if (!nomCompra.isBlank() && (datos.getNombres() == null || datos.getNombres().isBlank() || datos.getNombres().equals(usuario.getNombreUsuario()))) {
                String[] partes = nomCompra.split("\\s+", 2);
                datos.setNombres(partes[0]);
                if (partes.length > 1 && (datos.getApellidos() == null || datos.getApellidos().isBlank())) {
                    datos.setApellidos(partes[1]);
                }
                cambioDatos = true;
            }
        }
        if (cambioDatos) {
            datosRepo.save(datos);
        }

        Integer idDatos = datos.getIdDatosPersonales();

        if (dto.celularContacto() != null && !dto.celularContacto().isBlank()) {
            String numero = dto.celularContacto().trim();
            List<Telefono> listaTel = telefonos.findByDatosPersonales_IdDatosPersonales(idDatos);
            Telefono tel = listaTel.isEmpty() ? new Telefono() : listaTel.get(0);
            tel.setDatosPersonales(datos);
            tel.setNumeroTelefono(numero.length() > 20 ? numero.substring(0, 20) : numero);
            if (listaTel.isEmpty()) tel.setTipoTelefono("MOVIL");
            telefonos.save(tel);
        }

        // La dirección de entrega se guarda como copia en el pedido; NO debe sobrescribir la dirección permanente del perfil.
        // (Solo el envío/recojo/agencia usa estos datos; el perfil conserva su dirección registrada).

        if (dto.correoContacto() != null && !dto.correoContacto().isBlank()) {
            List<CorreoElectronico> listaCorreo = correos.findByDatosPersonales_IdDatosPersonales(idDatos);
            if (listaCorreo.isEmpty()) {
                String mail = dto.correoContacto().trim().toLowerCase();
                if (!correos.existsByCorreoElectronico(mail)) {
                    CorreoElectronico nuevo = new CorreoElectronico();
                    nuevo.setDatosPersonales(datos);
                    nuevo.setCorreoElectronico(mail.length() > 100 ? mail.substring(0, 100) : mail);
                    nuevo.setTipoCorreo("PERSONAL");
                    correos.save(nuevo);
                }
            }
        }
    }

    @Transactional
    public void cambiarPassword(Integer idUsuario, CambiarPasswordDTO request) {
        Usuario usuario = usuarios.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String actual = request.passwordActual() != null ? request.passwordActual() : "";
        String nueva = request.passwordNueva() != null ? request.passwordNueva().trim() : "";

        if (!encryptionStrategy.verificar(actual, usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        if (nueva.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (nueva.equals(actual)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
        }

        usuario.setContrasena(encryptionStrategy.encriptar(nueva));
        usuario.setRequiereCambioPassword(false);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarios.save(usuario);
    }
}
