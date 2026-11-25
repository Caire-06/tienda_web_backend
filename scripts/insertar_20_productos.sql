-- ============================================================================
-- TechStore - Inserción de 20 Productos Tecnológicos + Especificaciones 3FN
-- Base de datos objetivo: bd_sistema_negocio (PostgreSQL)
--
-- Características del script:
-- 1. Idempotente: No duplica registros al ejecutarse múltiples veces.
-- 2. Tercera Forma Normal (3FN): Productos en tabla 'productos' y ficha
--    técnica normalizada en 'especificaciones_tecnicas' (1:N).
-- 3. Auditoría obligatoria TechStore (fecha_creacion, usuario_registro).
-- ============================================================================

-- Asegurar columnas de auditoría y marca en productos si no existen
ALTER TABLE productos ADD COLUMN IF NOT EXISTS marca VARCHAR(100) DEFAULT 'Genérico';
ALTER TABLE productos ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) DEFAULT 'SISTEMA';

-- Asegurar tabla de especificaciones técnicas (3FN) con auditoría TechStore
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

-- Asegurar valores DEFAULT en caso la tabla ya existiera creada por Hibernate sin defaults
ALTER TABLE especificaciones_tecnicas ALTER COLUMN activo SET DEFAULT TRUE;
ALTER TABLE especificaciones_tecnicas ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE especificaciones_tecnicas ALTER COLUMN usuario_registro SET DEFAULT 'SISTEMA';

-- Asegurar integridad referencial (Foreign Keys) para visualización en Diagramas ER (DBeaver / pgAdmin)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'detalle_pedidos') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_detalle_pedidos_producto') THEN
            ALTER TABLE detalle_pedidos
                ADD CONSTRAINT fk_detalle_pedidos_producto
                FOREIGN KEY (producto_id) REFERENCES productos(id_producto);
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'detalle_entradas') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_detalle_entradas_producto') THEN
            ALTER TABLE detalle_entradas
                ADD CONSTRAINT fk_detalle_entradas_producto
                FOREIGN KEY (producto_id) REFERENCES productos(id_producto);
        END IF;
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 1. Inserción de Productos
-- ----------------------------------------------------------------------------
INSERT INTO productos (
    nombre,
    categoria,
    precio,
    stock,
    vendidos,
    imagen,
    descripcion,
    activo,
    marca,
    usuario_registro
)
SELECT
    v.nombre,
    v.categoria,
    v.precio,
    v.stock,
    v.vendidos,
    v.imagen,
    v.descripcion,
    v.activo,
    v.marca,
    'SISTEMA'
FROM (
    VALUES
        -- Smartphones (4)
        (
            'Xiaomi Redmi Note 13 Pro 5G 256GB',
            'Smartphones',
            1299.00,
            18,
            0,
            NULL::varchar(300),
            'Pantalla AMOLED 120Hz de 6.67 pulgadas, cámara principal de 200MP con OIS, procesador Snapdragon 7s Gen 2 y carga turbo de 67W.',
            TRUE,
            'Xiaomi'
        ),
        (
            'Motorola Edge 50 Fusion 8GB / 256GB',
            'Smartphones',
            1399.00,
            14,
            0,
            NULL::varchar(300),
            'Diseño ultradelgado con protección IP68, pantalla pOLED curva de 144Hz, 256GB de almacenamiento y batería de 5000mAh.',
            TRUE,
            'Motorola'
        ),
        (
            'HONOR Magic6 Lite 5G 256GB',
            'Smartphones',
            1149.00,
            20,
            0,
            NULL::varchar(300),
            'Pantalla AMOLED ultrarresistente 1.5K, batería de larga duración de 5800mAh, cámara triple de 108MP y conectividad 5G.',
            TRUE,
            'HONOR'
        ),
        (
            'Apple iPhone 15 128GB Negro',
            'Smartphones',
            3699.00,
            7,
            0,
            NULL::varchar(300),
            'Chip A16 Bionic, Dynamic Island, pantalla Super Retina XDR OLED de 6.1 pulgadas, cámara principal de 48MP y puerto USB-C.',
            TRUE,
            'Apple'
        ),

        -- Audio (4)
        (
            'Audífonos HyperX Cloud III Cableados',
            'Audio',
            369.00,
            16,
            0,
            NULL::varchar(300),
            'Drivers angulados de 53mm sintonizados para gaming, micrófono desmontable con cancelación de ruido y almohadillas viscoelásticas.',
            TRUE,
            'HyperX'
        ),
        (
            'Audífonos Samsung Galaxy Buds FE',
            'Audio',
            289.00,
            22,
            0,
            NULL::varchar(300),
            'Auriculares inalámbricos TWS con cancelación activa de ruido ANC, ajuste ergonómico con aletas y hasta 30 horas de autonomía.',
            TRUE,
            'Samsung'
        ),
        (
            'Barra de Sonido LG S40T 2.1 Canales 300W',
            'Audio',
            599.00,
            9,
            0,
            NULL::varchar(300),
            'Soundbar con subwoofer inalámbrico, audio Dolby Digital y DTS, conectividad Bluetooth 5.3 y HDMI ARC para sala o escritorio.',
            TRUE,
            'LG'
        ),
        (
            'Micrófono Condensador Fifine AM8 XLR/USB',
            'Audio',
            249.00,
            15,
            0,
            NULL::varchar(300),
            'Micrófono dinámico dual USB-C y XLR para streaming y podcasts, botón táctil de silencio, monitoreo sin latencia e iluminación RGB.',
            TRUE,
            'Fifine'
        ),

        -- Periféricos (4)
        (
            'Mouse Razer DeathAdder Essential Blanco',
            'Periféricos',
            129.00,
            25,
            0,
            NULL::varchar(300),
            'Sensor óptico de 6400 DPI, 5 botones programables Hyperesponse y switches mecánicos duraderos con diseño ergonómico clásico.',
            TRUE,
            'Razer'
        ),
        (
            'Teclado Mecánico Logitech G413 SE TKL',
            'Periféricos',
            279.00,
            12,
            0,
            NULL::varchar(300),
            'Teclado compacto sin pad numérico con teclas PBT resistentes al desgaste, switches táctiles y carcasa superior de aluminio cepillado.',
            TRUE,
            'Logitech'
        ),
        (
            'Webcam Logitech C920s Pro Full HD 1080p',
            'Periféricos',
            299.00,
            14,
            0,
            NULL::varchar(300),
            'Lente de cristal Full HD 1080p a 30fps con enfoque automático HD, corrección de iluminación automática y tapa de privacidad integrada.',
            TRUE,
            'Logitech'
        ),
        (
            'Pad Mouse Corsair MM300 Pro Extended',
            'Periféricos',
            119.00,
            30,
            0,
            NULL::varchar(300),
            'Alfombrilla tamaño extendido (930 x 300 mm) con revestimiento microtejido resistente a derrames y bordes cosidos antideshilachado.',
            TRUE,
            'Corsair'
        ),

        -- Laptops (4)
        (
            'Laptop ASUS Vivobook 15 Core i5 12va / 16GB / 512GB',
            'Laptops',
            2399.00,
            8,
            0,
            NULL::varchar(300),
            'Procesador Intel Core i5-1235U, 16GB RAM DDR4, SSD NVMe 512GB, pantalla Full HD antirreflejo de 15.6 pulgadas y bisagra de 180 grados.',
            TRUE,
            'ASUS'
        ),
        (
            'Laptop HP Victus 15 Ryzen 5 / RTX 2050 / 16GB / 512GB',
            'Laptops',
            3299.00,
            6,
            0,
            NULL::varchar(300),
            'Equipo gamer con AMD Ryzen 5 7535HS, gráficos NVIDIA GeForce RTX 2050 4GB, pantalla IPS 144Hz de 15.6 pulgadas y teclado retroiluminado.',
            TRUE,
            'HP'
        ),
        (
            'Monitor LG UltraGear 24GS60F 24 Pulgadas IPS 180Hz',
            'Periféricos',
            699.00,
            11,
            0,
            NULL::varchar(300),
            'Panel IPS Full HD con frecuencia de actualización de 180Hz, tiempo de respuesta 1ms GtG, compatibilidad NVIDIA G-Sync y AMD FreeSync.',
            TRUE,
            'LG'
        ),
        (
            'Mini PC Beelink SER5 Ryzen 7 5800H / 16GB / 500GB',
            'Laptops',
            1699.00,
            5,
            0,
            NULL::varchar(300),
            'Computadora de escritorio ultracompacta con Ryzen 7 5800H, Wi-Fi 6, Bluetooth 5.2, doble salida HDMI y puerto USB-C frontal.',
            TRUE,
            'Beelink'
        ),

        -- Accesorios (4)
        (
            'Hub USB-C UGREEN 6 en 1 HDMI 4K y Ethernet',
            'Accesorios',
            139.00,
            24,
            0,
            NULL::varchar(300),
            'Adaptador multipuerto de aluminio con salida HDMI 4K 60Hz, puerto RJ45 Gigabit Ethernet, lector SD/TF y 3 puertos USB 3.0.',
            TRUE,
            'UGREEN'
        ),
        (
            'SSD Externo Kingston XS1000 1TB USB 3.2 Gen 2',
            'Accesorios',
            349.00,
            13,
            0,
            NULL::varchar(300),
            'Unidad de estado sólido portátil ultracompacta con velocidades de lectura de hasta 1050 MB/s e interfaz USB-C / USB-A incluida.',
            TRUE,
            'Kingston'
        ),
        (
            'Power Bank Anker 20000mAh Carga Rápida 22.5W',
            'Accesorios',
            189.00,
            19,
            0,
            NULL::varchar(300),
            'Batería externa de alta capacidad con puerto bidireccional USB-C, doble salida USB-A y protección térmica ActiveShield.',
            TRUE,
            'Anker'
        ),
        (
            'Mochila Targus Intellect Essentials 15.6 Pulgadas',
            'Accesorios',
            99.00,
            28,
            0,
            NULL::varchar(300),
            'Mochila ligera repelente al agua con compartimento acolchado para laptops de hasta 15.6 pulgadas, correas ajustables y bolsillo frontal.',
            TRUE,
            'Targus'
        )
) AS v(nombre, categoria, precio, stock, vendidos, imagen, descripcion, activo, marca)
WHERE NOT EXISTS (
    SELECT 1
    FROM productos p
    WHERE p.nombre = v.nombre
);

-- Si existían con la categoría antigua 'Computadoras', actualizar a 'Laptops'
UPDATE productos SET categoria = 'Laptops' WHERE categoria = 'Computadoras';

-- ----------------------------------------------------------------------------
-- 2. Inserción de Especificaciones Técnicas (3FN) para todos los productos
-- ----------------------------------------------------------------------------

-- Helper: insertar especificación si no existe
CREATE OR REPLACE FUNCTION insertar_spec(prod_nombre TEXT, caract TEXT, val TEXT)
RETURNS VOID AS $$
BEGIN
    INSERT INTO especificaciones_tecnicas (
        id_producto,
        caracteristica,
        valor,
        activo,
        fecha_creacion,
        usuario_registro
    )
    SELECT
        p.id_producto,
        caract,
        val,
        TRUE,
        CURRENT_TIMESTAMP,
        'SISTEMA'
    FROM productos p
    WHERE p.nombre = prod_nombre
      AND NOT EXISTS (
          SELECT 1 FROM especificaciones_tecnicas e
          WHERE e.id_producto = p.id_producto AND LOWER(e.caracteristica) = LOWER(caract)
      );
END;
$$ LANGUAGE plpgsql;

-- Especificaciones: Smartphones
SELECT insertar_spec('Xiaomi Redmi Note 13 Pro 5G 256GB', 'Memoria RAM', '8 GB');
SELECT insertar_spec('Xiaomi Redmi Note 13 Pro 5G 256GB', 'Almacenamiento Interno', '256 GB');
SELECT insertar_spec('Xiaomi Redmi Note 13 Pro 5G 256GB', 'Capacidad de Batería', '5000 mAh');
SELECT insertar_spec('Xiaomi Redmi Note 13 Pro 5G 256GB', 'Tipo y Tasa de Pantalla', '6.67" AMOLED 120Hz');
SELECT insertar_spec('Xiaomi Redmi Note 13 Pro 5G 256GB', 'Conectividad Móvil', '5G Dual SIM');
SELECT insertar_spec('Xiaomi Redmi Note 13 Pro 5G 256GB', 'Potencia de Carga Rápida', '67W');

SELECT insertar_spec('Motorola Edge 50 Fusion 8GB / 256GB', 'Memoria RAM', '8 GB');
SELECT insertar_spec('Motorola Edge 50 Fusion 8GB / 256GB', 'Almacenamiento Interno', '256 GB');
SELECT insertar_spec('Motorola Edge 50 Fusion 8GB / 256GB', 'Capacidad de Batería', '5000 mAh');
SELECT insertar_spec('Motorola Edge 50 Fusion 8GB / 256GB', 'Tipo y Tasa de Pantalla', '6.7" Dynamic AMOLED 2X 120Hz');
SELECT insertar_spec('Motorola Edge 50 Fusion 8GB / 256GB', 'Conectividad Móvil', '5G + eSIM');
SELECT insertar_spec('Motorola Edge 50 Fusion 8GB / 256GB', 'Potencia de Carga Rápida', '68W');

SELECT insertar_spec('HONOR Magic6 Lite 5G 256GB', 'Memoria RAM', '8 GB');
SELECT insertar_spec('HONOR Magic6 Lite 5G 256GB', 'Almacenamiento Interno', '256 GB');
SELECT insertar_spec('HONOR Magic6 Lite 5G 256GB', 'Capacidad de Batería', '5800 mAh');
SELECT insertar_spec('HONOR Magic6 Lite 5G 256GB', 'Tipo y Tasa de Pantalla', '6.78" AMOLED 120Hz');
SELECT insertar_spec('HONOR Magic6 Lite 5G 256GB', 'Conectividad Móvil', '5G Dual SIM');
SELECT insertar_spec('HONOR Magic6 Lite 5G 256GB', 'Potencia de Carga Rápida', '35W');

SELECT insertar_spec('Apple iPhone 15 128GB Negro', 'Memoria RAM', '6 GB');
SELECT insertar_spec('Apple iPhone 15 128GB Negro', 'Almacenamiento Interno', '128 GB');
SELECT insertar_spec('Apple iPhone 15 128GB Negro', 'Capacidad de Batería', '3349 mAh');
SELECT insertar_spec('Apple iPhone 15 128GB Negro', 'Tipo y Tasa de Pantalla', '6.1" Super Retina XDR');
SELECT insertar_spec('Apple iPhone 15 128GB Negro', 'Conectividad Móvil', '5G + eSIM');
SELECT insertar_spec('Apple iPhone 15 128GB Negro', 'Potencia de Carga Rápida', '20W');

-- Especificaciones: Audio
SELECT insertar_spec('Audífonos HyperX Cloud III Cableados', 'Tipo de Diseño', 'Over-Ear (Diadema)');
SELECT insertar_spec('Audífonos HyperX Cloud III Cableados', 'Conectividad Principal', 'Cable Jack 3.5mm');
SELECT insertar_spec('Audífonos HyperX Cloud III Cableados', 'Cancelación Activa de Ruido', 'Cancelación Pasiva');
SELECT insertar_spec('Audífonos HyperX Cloud III Cableados', 'Micrófono Integrado', 'Micrófono Desmontable Gamer');

SELECT insertar_spec('Audífonos Samsung Galaxy Buds FE', 'Tipo de Diseño', 'In-Ear (Intraural)');
SELECT insertar_spec('Audífonos Samsung Galaxy Buds FE', 'Conectividad Principal', 'Bluetooth 5.3');
SELECT insertar_spec('Audífonos Samsung Galaxy Buds FE', 'Cancelación Activa de Ruido', 'Sí (ANC Activa)');
SELECT insertar_spec('Audífonos Samsung Galaxy Buds FE', 'Autonomía de Batería', '30 horas');
SELECT insertar_spec('Audífonos Samsung Galaxy Buds FE', 'Micrófono Integrado', 'Integrado con Reducción de Ruido (ENC)');

SELECT insertar_spec('Barra de Sonido LG S40T 2.1 Canales 300W', 'Tipo de Diseño', 'Parlante Portátil');
SELECT insertar_spec('Barra de Sonido LG S40T 2.1 Canales 300W', 'Conectividad Principal', 'Bluetooth 5.3');
SELECT insertar_spec('Barra de Sonido LG S40T 2.1 Canales 300W', 'Cancelación Activa de Ruido', 'No');

SELECT insertar_spec('Micrófono Condensador Fifine AM8 XLR/USB', 'Tipo de Diseño', 'In-Ear (Intraural)');
SELECT insertar_spec('Micrófono Condensador Fifine AM8 XLR/USB', 'Conectividad Principal', 'USB Type-C');
SELECT insertar_spec('Micrófono Condensador Fifine AM8 XLR/USB', 'Micrófono Integrado', 'Integrado con Reducción de Ruido (ENC)');

-- Especificaciones: Periféricos
SELECT insertar_spec('Mouse Razer DeathAdder Essential Blanco', 'Conectividad Principal', 'Cable Jack 3.5mm');
SELECT insertar_spec('Teclado Mecánico Logitech G413 SE TKL', 'Conectividad Principal', 'Cable Jack 3.5mm');
SELECT insertar_spec('Webcam Logitech C920s Pro Full HD 1080p', 'Conectividad Principal', 'USB Type-C');
SELECT insertar_spec('Monitor LG UltraGear 24GS60F 24 Pulgadas IPS 180Hz', 'Conectividad Principal', 'HDMI / DisplayPort');

-- Especificaciones: Laptops
SELECT insertar_spec('Laptop ASUS Vivobook 15 Core i5 12va / 16GB / 512GB', 'Tipo de Laptop', 'Laptop para Trabajo / Oficina');
SELECT insertar_spec('Laptop ASUS Vivobook 15 Core i5 12va / 16GB / 512GB', 'Memoria RAM', '16 GB DDR4');
SELECT insertar_spec('Laptop ASUS Vivobook 15 Core i5 12va / 16GB / 512GB', 'Almacenamiento SSD', '512 GB SSD NVMe');
SELECT insertar_spec('Laptop ASUS Vivobook 15 Core i5 12va / 16GB / 512GB', 'Procesador', 'Intel Core i5 (13va Gen)');

SELECT insertar_spec('Laptop HP Victus 15 Ryzen 5 / RTX 2050 / 16GB / 512GB', 'Tipo de Laptop', 'Laptop Gamer');
SELECT insertar_spec('Laptop HP Victus 15 Ryzen 5 / RTX 2050 / 16GB / 512GB', 'Memoria RAM', '16 GB DDR4');
SELECT insertar_spec('Laptop HP Victus 15 Ryzen 5 / RTX 2050 / 16GB / 512GB', 'Almacenamiento SSD', '512 GB SSD NVMe');
SELECT insertar_spec('Laptop HP Victus 15 Ryzen 5 / RTX 2050 / 16GB / 512GB', 'Procesador', 'AMD Ryzen 5 Serie 7000');

SELECT insertar_spec('Mini PC Beelink SER5 Ryzen 7 5800H / 16GB / 500GB', 'Tipo de Laptop', 'Laptop para Trabajo / Oficina');
SELECT insertar_spec('Mini PC Beelink SER5 Ryzen 7 5800H / 16GB / 500GB', 'Memoria RAM', '16 GB DDR4');
SELECT insertar_spec('Mini PC Beelink SER5 Ryzen 7 5800H / 16GB / 500GB', 'Almacenamiento SSD', '512 GB SSD NVMe');
SELECT insertar_spec('Mini PC Beelink SER5 Ryzen 7 5800H / 16GB / 500GB', 'Procesador', 'AMD Ryzen 7 Serie 7000/8000');

-- Especificaciones: Accesorios
SELECT insertar_spec('Hub USB-C UGREEN 6 en 1 HDMI 4K y Ethernet', 'Conectividad Principal', 'USB Type-C');
SELECT insertar_spec('SSD Externo Kingston XS1000 1TB USB 3.2 Gen 2', 'Conectividad Principal', 'USB Type-C');
SELECT insertar_spec('Power Bank Anker 20000mAh Carga Rápida 22.5W', 'Capacidad de Batería', '20000 mAh');
SELECT insertar_spec('Power Bank Anker 20000mAh Carga Rápida 22.5W', 'Potencia de Carga Rápida', '22.5W');

-- Limpiar función temporal
DROP FUNCTION IF EXISTS insertar_spec(TEXT, TEXT, TEXT);

-- Fin del script.
