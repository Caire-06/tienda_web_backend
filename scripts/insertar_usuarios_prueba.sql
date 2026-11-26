-- Usuarios temporales de verificación para TechStore (sin correo electrónico).
-- Contraseña para los tres: Temp1234!  (hash SHA-256, igual que Sha256EncryptionStrategy)
-- Ejecutar en: bd_sistema_negocio
-- El código Java fue restaurado a su estado original; estas cuentas solo se crean
-- al ejecutar este script. Bórralas con las sentencias comentadas al final.

BEGIN;

INSERT INTO rol (nombre_rol, descripcion)
SELECT 'VENDEDOR', 'Atención y ventas al cliente'
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol = 'VENDEDOR');

INSERT INTO permiso (nombre_permiso, descripcion)
SELECT 'TODO', 'Acceso total a todos los módulos'
WHERE NOT EXISTS (SELECT 1 FROM permiso WHERE nombre_permiso = 'TODO');

-- admin_prueba -> ADMINISTRADOR
INSERT INTO usuarios (
    nombre_usuario, contrasena, rol_id, permiso_id, ultimo_acceso,
    intentos_fallidos, bloqueado, bloqueado_hasta, nivel_bloqueo,
    token_recuperacion, estado, fecha_creacion, fecha_actualizacion,
    usuario_registro, requiere_cambio_password, correo_verificado, two_factor_activo
)
SELECT
    'admin_prueba',
    '54f370ce7311f24ccd3ce4deac7f1a8c2ca88151598f7d4321ec275d4a987ed5',
    r.rol_id,
    p.permiso_id,
    NULL, 0, FALSE, NULL, 0, NULL, 'Activo', NOW(), NOW(), 'SISTEMA', FALSE, TRUE, FALSE
FROM rol r CROSS JOIN permiso p
WHERE r.nombre_rol = 'ADMINISTRADOR' AND p.nombre_permiso = 'TODO'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.nombre_usuario = 'admin_prueba');

-- almacen_prueba -> ALMACEN
INSERT INTO usuarios (
    nombre_usuario, contrasena, rol_id, permiso_id, ultimo_acceso,
    intentos_fallidos, bloqueado, bloqueado_hasta, nivel_bloqueo,
    token_recuperacion, estado, fecha_creacion, fecha_actualizacion,
    usuario_registro, requiere_cambio_password, correo_verificado, two_factor_activo
)
SELECT
    'almacen_prueba',
    '54f370ce7311f24ccd3ce4deac7f1a8c2ca88151598f7d4321ec275d4a987ed5',
    r.rol_id,
    p.permiso_id,
    NULL, 0, FALSE, NULL, 0, NULL, 'Activo', NOW(), NOW(), 'SISTEMA', FALSE, TRUE, FALSE
FROM rol r CROSS JOIN permiso p
WHERE r.nombre_rol = 'ALMACEN' AND p.nombre_permiso = 'TODO'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.nombre_usuario = 'almacen_prueba');

-- vendedor_prueba -> VENDEDOR
INSERT INTO usuarios (
    nombre_usuario, contrasena, rol_id, permiso_id, ultimo_acceso,
    intentos_fallidos, bloqueado, bloqueado_hasta, nivel_bloqueo,
    token_recuperacion, estado, fecha_creacion, fecha_actualizacion,
    usuario_registro, requiere_cambio_password, correo_verificado, two_factor_activo
)
SELECT
    'vendedor_prueba',
    '54f370ce7311f24ccd3ce4deac7f1a8c2ca88151598f7d4321ec275d4a987ed5',
    r.rol_id,
    p.permiso_id,
    NULL, 0, FALSE, NULL, 0, NULL, 'Activo', NOW(), NOW(), 'SISTEMA', FALSE, TRUE, FALSE
FROM rol r CROSS JOIN permiso p
WHERE r.nombre_rol = 'VENDEDOR' AND p.nombre_permiso = 'TODO'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.nombre_usuario = 'vendedor_prueba');

-- Datos personales mínimos: el frontend valida la sesión con GET /api/perfil.
-- No se insertan filas en correo_electronico; las cuentas siguen sin correo.
INSERT INTO sexo (descripcion)
SELECT 'No Especifica'
WHERE NOT EXISTS (SELECT 1 FROM sexo WHERE descripcion = 'No Especifica');

INSERT INTO estado_civil (descripcion)
SELECT 'Soltero(a)'
WHERE NOT EXISTS (SELECT 1 FROM estado_civil WHERE descripcion = 'Soltero(a)');

INSERT INTO datos_personales (nombres, apellidos, dni, id_estado_civil, id_sexo, id_usuario)
SELECT 'Admin', 'Prueba', '00000001', ec.id_estado_civil, s.id_sexo, u.id_usuario
FROM usuarios u CROSS JOIN sexo s CROSS JOIN estado_civil ec
WHERE u.nombre_usuario = 'admin_prueba'
  AND s.descripcion = 'No Especifica' AND ec.descripcion = 'Soltero(a)'
  AND NOT EXISTS (SELECT 1 FROM datos_personales d WHERE d.id_usuario = u.id_usuario);

INSERT INTO datos_personales (nombres, apellidos, dni, id_estado_civil, id_sexo, id_usuario)
SELECT 'Almacen', 'Prueba', '00000002', ec.id_estado_civil, s.id_sexo, u.id_usuario
FROM usuarios u CROSS JOIN sexo s CROSS JOIN estado_civil ec
WHERE u.nombre_usuario = 'almacen_prueba'
  AND s.descripcion = 'No Especifica' AND ec.descripcion = 'Soltero(a)'
  AND NOT EXISTS (SELECT 1 FROM datos_personales d WHERE d.id_usuario = u.id_usuario);

INSERT INTO datos_personales (nombres, apellidos, dni, id_estado_civil, id_sexo, id_usuario)
SELECT 'Vendedor', 'Prueba', '00000003', ec.id_estado_civil, s.id_sexo, u.id_usuario
FROM usuarios u CROSS JOIN sexo s CROSS JOIN estado_civil ec
WHERE u.nombre_usuario = 'vendedor_prueba'
  AND s.descripcion = 'No Especifica' AND ec.descripcion = 'Soltero(a)'
  AND NOT EXISTS (SELECT 1 FROM datos_personales d WHERE d.id_usuario = u.id_usuario);

COMMIT;

-- Verificación:
-- SELECT u.nombre_usuario, r.nombre_rol, u.correo_verificado
-- FROM usuarios u JOIN rol r ON r.rol_id = u.rol_id
-- WHERE u.nombre_usuario LIKE '%_prueba';

-- Limpieza (cuando ya no se necesiten):
-- DELETE FROM datos_personales WHERE id_usuario IN (
--     SELECT id_usuario FROM usuarios WHERE nombre_usuario IN ('admin_prueba', 'almacen_prueba', 'vendedor_prueba'));
-- DELETE FROM usuarios WHERE nombre_usuario IN ('admin_prueba', 'almacen_prueba', 'vendedor_prueba');
-- DELETE FROM rol WHERE nombre_rol = 'VENDEDOR' AND NOT EXISTS (SELECT 1 FROM usuarios WHERE rol_id = (SELECT rol_id FROM rol WHERE nombre_rol = 'VENDEDOR'));
