-- ============================================================================
-- TechStore - Especificaciones técnicas de productos (3FN) + auditoría
-- Base de datos: bd_sistema_negocio (PostgreSQL)
-- Ejecutar este script DESPUÉS de arrancar el backend una primera vez
-- (ddl-auto=update crea la tabla productos). Luego reinicia el backend para que
-- Hibernate use las columnas de auditoría y marca ya existentes.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Tabla independiente 1:N de especificaciones.
--    CERO columnas NULL: valor y magnitud se guardan juntos en `valor`.
--    No se agrega ninguna columna de especificación a la tabla productos.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS especificaciones_tecnicas (
    id_especificacion   SERIAL PRIMARY KEY,
    id_producto         INT NOT NULL,
    caracteristica      VARCHAR(100) NOT NULL,
    valor               VARCHAR(150) NOT NULL,

    -- Auditoría obligatoria TechStore
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    usuario_registro    VARCHAR(50) NOT NULL DEFAULT 'SISTEMA',
    activo              BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_especificaciones_producto
        FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_especificaciones_producto
    ON especificaciones_tecnicas(id_producto);

-- ----------------------------------------------------------------------------
-- 2) Columnas de apoyo en `productos` (NO son especificaciones técnicas):
--    - marca: atributo propio del producto (depende solo de id_producto -> 3FN).
--    - fecha_creacion / fecha_actualizacion / usuario_registro: auditoría.
--    Se usa DEFAULT para que los productos ya existentes no queden en NULL.
-- ----------------------------------------------------------------------------
ALTER TABLE productos ADD COLUMN IF NOT EXISTS marca VARCHAR(100) DEFAULT 'Genérico';
ALTER TABLE productos ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) DEFAULT 'SISTEMA';

UPDATE productos SET marca = 'Genérico' WHERE marca IS NULL OR BTRIM(marca) = '';
UPDATE productos SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE productos SET usuario_registro = 'SISTEMA' WHERE usuario_registro IS NULL OR BTRIM(usuario_registro) = '';

-- Marca inferida de los nombres ya registrados (solo datos de prueba).
UPDATE productos SET marca = 'Logitech' WHERE nombre ILIKE '%Logitech%';
UPDATE productos SET marca = 'Sony'     WHERE nombre ILIKE '%Sony%';
UPDATE productos SET marca = 'Redragon' WHERE nombre ILIKE '%Redragon%';
UPDATE productos SET marca = 'Lenovo'   WHERE nombre ILIKE '%Lenovo%';
UPDATE productos SET marca = 'Samsung'  WHERE nombre ILIKE '%Samsung%';
UPDATE productos SET marca = 'Xiaomi'   WHERE nombre ILIKE '%Xiaomi%';
UPDATE productos SET marca = 'JBL'      WHERE nombre ILIKE '%JBL%';
UPDATE productos SET marca = 'Apple'    WHERE nombre ILIKE '%Apple%' OR nombre ILIKE '%iPhone%' OR nombre ILIKE '%MacBook%';

-- ----------------------------------------------------------------------------
-- 3) Datos de prueba de la ficha técnica (idempotente: no duplica al repetir).
--    Los productos se identifican por nombre, no por ID hardcodeado.
-- ----------------------------------------------------------------------------
INSERT INTO especificaciones_tecnicas
    (id_producto, caracteristica, valor, usuario_registro)
SELECT p.id_producto, v.caracteristica, v.valor, 'SISTEMA'
FROM productos p
CROSS JOIN (VALUES
    ('Memoria RAM', '8 GB'),
    ('Almacenamiento', '128 GB'),
    ('Batería', '5000 mAh'),
    ('Pantalla', '6.6" Super AMOLED 120Hz'),
    ('Procesador', 'Exynos 1480'),
    ('Conectividad', '5G Dual SIM')
) AS v(caracteristica, valor)
WHERE p.nombre = 'Samsung Galaxy A55 5G 8GB / 128GB'
  AND NOT EXISTS (
      SELECT 1 FROM especificaciones_tecnicas e
      WHERE e.id_producto = p.id_producto AND e.caracteristica = v.caracteristica
  );

INSERT INTO especificaciones_tecnicas
    (id_producto, caracteristica, valor, usuario_registro)
SELECT p.id_producto, v.caracteristica, v.valor, 'SISTEMA'
FROM productos p
CROSS JOIN (VALUES
    ('Tipo de Diseño', 'Over-Ear (Diadema)'),
    ('Cancelación de Ruido', 'Sí (ANC Activa)'),
    ('Autonomía', '30 horas'),
    ('Conectividad', 'Bluetooth 5.3 y Cable 3.5mm'),
    ('Micrófono', 'Integrado con reducción de ruido')
) AS v(caracteristica, valor)
WHERE p.nombre = 'Audífonos Sony WH-1000XM4'
  AND NOT EXISTS (
      SELECT 1 FROM especificaciones_tecnicas e
      WHERE e.id_producto = p.id_producto AND e.caracteristica = v.caracteristica
  );

INSERT INTO especificaciones_tecnicas
    (id_producto, caracteristica, valor, usuario_registro)
SELECT p.id_producto, v.caracteristica, v.valor, 'SISTEMA'
FROM productos p
CROSS JOIN (VALUES
    ('Procesador', 'AMD Ryzen 7 serie 5000'),
    ('Memoria RAM', '16 GB DDR4'),
    ('Almacenamiento', '512 GB SSD'),
    ('Pantalla', '15.6" FHD'),
    ('Sistema Operativo', 'Windows 11 Home')
) AS v(caracteristica, valor)
WHERE p.nombre = 'Laptop Lenovo IdeaPad Slim 3 Ryzen 7 / 16GB / 512GB'
  AND NOT EXISTS (
      SELECT 1 FROM especificaciones_tecnicas e
      WHERE e.id_producto = p.id_producto AND e.caracteristica = v.caracteristica
  );

-- Fin del script.
