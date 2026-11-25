-- ============================================================================
-- TechStore - Inserción de Proveedores Tecnológicos Mayoristas (3FN)
-- Base de datos objetivo: bd_sistema_negocio (PostgreSQL / Neon Tech)
--
-- Características del script:
-- 1. Idempotente: No duplica registros al ejecutarse múltiples veces (filtra por RUC).
-- 2. Empresas reales: Distribuidores mayoristas autorizados de tecnología en Perú.
-- 3. Auditoría obligatoria TechStore (fecha_creacion, fecha_actualizacion, activo).
-- ============================================================================

-- 1. Asegurar estructura de la tabla proveedores si no existiera
CREATE TABLE IF NOT EXISTS proveedores (
    id_proveedor        SERIAL PRIMARY KEY,
    nombre              VARCHAR(200) NOT NULL,
    ruc                 VARCHAR(20) UNIQUE,
    contacto            VARCHAR(100),
    telefono            VARCHAR(30),
    email               VARCHAR(120),
    direccion           VARCHAR(300),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);

-- 2. Asegurar columnas de auditoría en caso de tablas creadas previamente
ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;

-- 3. Inserción de Proveedores Mayoristas Idempotente
INSERT INTO proveedores (
    nombre,
    ruc,
    contacto,
    telefono,
    email,
    direccion,
    activo,
    fecha_creacion,
    fecha_actualizacion
)
SELECT
    v.nombre,
    v.ruc,
    v.contacto,
    v.telefono,
    v.email,
    v.direccion,
    v.activo,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    VALUES
        (
            'Grupo Deltron S.A.',
            '20212301465',
            'Carlos Mendoza (Gerente de Cuentas)',
            '+51 (01) 415-0100',
            'ventas@deltron.com.pe',
            'Av. Manuel Gómez 245, Lince, Lima',
            TRUE
        ),
        (
            'Ingram Micro Perú S.A.C.',
            '20387532671',
            'Mariana Silva (Ejecutiva Comercial)',
            '+51 (01) 618-9000',
            'contacto.peru@ingrammicro.com',
            'Av. Rivera Navarrete 501 Piso 14, San Isidro, Lima',
            TRUE
        ),
        (
            'Máxima Internacional S.A.',
            '20254921612',
            'Jorge Ramos (Distribución Mayorista)',
            '+51 (01) 619-3300',
            'ventas@maximainternacional.com',
            'Calle Los Ficus 180, Miraflores, Lima',
            TRUE
        ),
        (
            'Intcomex del Perú S.A.C.',
            '20346387291',
            'Vanessa Torres (Ventas Corporativas)',
            '+51 (01) 710-5000',
            'ventas.pe@intcomex.com',
            'Av. Materiales 3049, Cercado de Lima',
            TRUE
        ),
        (
            'Tech Data Perú S.A.C.',
            '20491823764',
            'Roberto Gutiérrez (Asesor Mayorista)',
            '+51 (01) 512-4200',
            'corporativo@techdata.pe',
            'Av. Víctor Andrés Belaúnde 147, San Isidro, Lima',
            TRUE
        ),
        (
            'Compudiskett S.R.L.',
            '20101928374',
            'Elena Morales (Ventas y Accesorios)',
            '+51 (01) 433-2828',
            'ventas@compudiskett.com.pe',
            'Av. Bolivia 148, Centro Comercial Garcilaso, Lima',
            TRUE
        ),
        (
            'Importaciones Hiraoka S.A.C. (División Mayorista)',
            '20100021669',
            'Alberto Takahashi (Canal Distribución)',
            '+51 (01) 428-0270',
            'ventasmayor@hiraoka.com.pe',
            'Av. Abancay 594, Cercado de Lima',
            TRUE
        ),
        (
            'Nexus Technology S.A.C.',
            '20302826131',
            'Ricardo Benavides (Redes y Hardware)',
            '+51 (01) 617-5700',
            'contacto@nexus.com.pe',
            'Av. Aramburú 878, Surquillo, Lima',
            TRUE
        )
) AS v(nombre, ruc, contacto, telefono, email, direccion, activo)
WHERE NOT EXISTS (
    SELECT 1 FROM proveedores p WHERE p.ruc = v.ruc
);

-- Asegurar que todos los proveedores insertados queden con estado activo = TRUE
UPDATE proveedores SET activo = TRUE WHERE activo IS NULL;

-- Fin del script.
