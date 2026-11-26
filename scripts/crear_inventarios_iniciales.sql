-- Kardex: inventario inicial en 3FN
-- Ejecutar en: bd_sistema_negocio
--
-- La cantidad NO se pide dos veces: se toma del campo "stock" (Stock físico) del producto.
-- El costo unitario inicial es independiente del precio de venta y se registra por separado.
-- Hibernate (ddl-auto=update) también crea la tabla; este script la garantiza y documenta
-- la auditoría obligatoria y la relación 1:1 con productos.

CREATE TABLE IF NOT EXISTS inventarios_iniciales (
    id_inventario_inicial SERIAL PRIMARY KEY,
    producto_id           INTEGER        NOT NULL,
    cantidad              INTEGER        NOT NULL,
    costo_unitario        NUMERIC(12, 2) NOT NULL,
    fecha                 TIMESTAMP      NOT NULL,
    activo                BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_creacion        TIMESTAMP,
    fecha_actualizacion   TIMESTAMP,
    usuario_registro      VARCHAR(50),
    CONSTRAINT uq_inventario_inicial_producto UNIQUE (producto_id),
    CONSTRAINT fk_inventario_inicial_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id_producto) ON DELETE CASCADE,
    CONSTRAINT ck_inventario_inicial_cantidad CHECK (cantidad >= 0),
    CONSTRAINT ck_inventario_inicial_costo CHECK (costo_unitario >= 0)
);

CREATE INDEX IF NOT EXISTS idx_inventarios_iniciales_producto
    ON inventarios_iniciales (producto_id);

COMMENT ON TABLE inventarios_iniciales IS
    'Evento único de inventario inicial por producto; alimenta el Kardex con promedio ponderado móvil.';
COMMENT ON COLUMN inventarios_iniciales.cantidad IS
    'Copia histórica del stock físico del producto al crear o regularizar; no se recalcula con ventas posteriores.';
COMMENT ON COLUMN inventarios_iniciales.costo_unitario IS
    'Costo unitario inicial informado por Almacén/Admin. NO es el precio de venta.';
COMMENT ON COLUMN inventarios_iniciales.fecha IS
    'Fecha del evento: alta del producto o fecha de regularización de uno ya existente.';

-- NO se inserta ningún costo automáticamente: inventar costos falsificaría el Kardex.
-- Para regularizar productos ya existentes usa:
--   POST /api/kardex/inventario-inicial/regularizar
--   { "productoId": 1, "costoUnitario": 120.50 }
-- o el panel "Regularizar inventario inicial" en la vista Kardex del frontend.
