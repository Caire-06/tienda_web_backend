-- ============================================================================
-- Script idempotente: Inserción de 20 productos tecnológicos sin imagen
-- Base de datos objetivo: bd_sistema_negocio (PostgreSQL)
-- Ejecución recomendada:
--   psql -U postgres -d bd_sistema_negocio -f insertar_20_productos.sql
--   o abrir y ejecutar en pgAdmin Query Tool sobre bd_sistema_negocio.
-- ============================================================================

INSERT INTO productos (
    nombre,
    categoria,
    precio,
    stock,
    vendidos,
    imagen,
    descripcion,
    activo
)
SELECT
    v.nombre,
    v.categoria,
    v.precio,
    v.stock,
    v.vendidos,
    v.imagen,
    v.descripcion,
    v.activo
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
            TRUE
        ),
        (
            'Motorola Edge 50 Fusion 8GB / 256GB',
            'Smartphones',
            1399.00,
            14,
            0,
            NULL::varchar(300),
            'Diseño ultradelgado con protección IP68, pantalla pOLED curva de 144Hz, 256GB de almacenamiento y batería de 5000mAh.',
            TRUE
        ),
        (
            'HONOR Magic6 Lite 5G 256GB',
            'Smartphones',
            1149.00,
            20,
            0,
            NULL::varchar(300),
            'Pantalla AMOLED ultrarresistente 1.5K, batería de larga duración de 5800mAh, cámara triple de 108MP y conectividad 5G.',
            TRUE
        ),
        (
            'Apple iPhone 15 128GB Negro',
            'Smartphones',
            3699.00,
            7,
            0,
            NULL::varchar(300),
            'Chip A16 Bionic, Dynamic Island, pantalla Super Retina XDR OLED de 6.1 pulgadas, cámara principal de 48MP y puerto USB-C.',
            TRUE
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
            TRUE
        ),
        (
            'Audífonos Samsung Galaxy Buds FE',
            'Audio',
            289.00,
            22,
            0,
            NULL::varchar(300),
            'Auriculares inalámbricos TWS con cancelación activa de ruido ANC, ajuste ergonómico con aletas y hasta 30 horas de autonomía.',
            TRUE
        ),
        (
            'Barra de Sonido LG S40T 2.1 Canales 300W',
            'Audio',
            599.00,
            9,
            0,
            NULL::varchar(300),
            'Soundbar con subwoofer inalámbrico, audio Dolby Digital y DTS, conectividad Bluetooth 5.3 y HDMI ARC para sala o escritorio.',
            TRUE
        ),
        (
            'Micrófono Condensador Fifine AM8 XLR/USB',
            'Audio',
            249.00,
            15,
            0,
            NULL::varchar(300),
            'Micrófono dinámico dual USB-C y XLR para streaming y podcasts, botón táctil de silencio, monitoreo sin latencia e iluminación RGB.',
            TRUE
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
            TRUE
        ),
        (
            'Teclado Mecánico Logitech G413 SE TKL',
            'Periféricos',
            279.00,
            12,
            0,
            NULL::varchar(300),
            'Teclado compacto sin pad numérico con teclas PBT resistentes al desgaste, switches táctiles y carcasa superior de aluminio cepillado.',
            TRUE
        ),
        (
            'Webcam Logitech C920s Pro Full HD 1080p',
            'Periféricos',
            299.00,
            14,
            0,
            NULL::varchar(300),
            'Lente de cristal Full HD 1080p a 30fps con enfoque automático HD, corrección de iluminación automática y tapa de privacidad integrada.',
            TRUE
        ),
        (
            'Pad Mouse Corsair MM300 Pro Extended',
            'Periféricos',
            119.00,
            30,
            0,
            NULL::varchar(300),
            'Alfombrilla tamaño extendido (930 x 300 mm) con revestimiento microtejido resistente a derrames y bordes cosidos antideshilachado.',
            TRUE
        ),

        -- Computadoras (4)
        (
            'Laptop ASUS Vivobook 15 Core i5 12va / 16GB / 512GB',
            'Computadoras',
            2399.00,
            8,
            0,
            NULL::varchar(300),
            'Procesador Intel Core i5-1235U, 16GB RAM DDR4, SSD NVMe 512GB, pantalla Full HD antirreflejo de 15.6 pulgadas y bisagra de 180 grados.',
            TRUE
        ),
        (
            'Laptop HP Victus 15 Ryzen 5 / RTX 2050 / 16GB / 512GB',
            'Computadoras',
            3299.00,
            6,
            0,
            NULL::varchar(300),
            'Equipo gamer con AMD Ryzen 5 7535HS, gráficos NVIDIA GeForce RTX 2050 4GB, pantalla IPS 144Hz de 15.6 pulgadas y teclado retroiluminado.',
            TRUE
        ),
        (
            'Monitor LG UltraGear 24GS60F 24 Pulgadas IPS 180Hz',
            'Computadoras',
            699.00,
            11,
            0,
            NULL::varchar(300),
            'Panel IPS Full HD con frecuencia de actualización de 180Hz, tiempo de respuesta 1ms GtG, compatibilidad NVIDIA G-Sync y AMD FreeSync.',
            TRUE
        ),
        (
            'Mini PC Beelink SER5 Ryzen 7 5800H / 16GB / 500GB',
            'Computadoras',
            1699.00,
            5,
            0,
            NULL::varchar(300),
            'Computadora de escritorio ultracompacta con Ryzen 7 5800H, Wi-Fi 6, Bluetooth 5.2, doble salida HDMI y puerto USB-C frontal.',
            TRUE
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
            TRUE
        ),
        (
            'SSD Externo Kingston XS1000 1TB USB 3.2 Gen 2',
            'Accesorios',
            349.00,
            13,
            0,
            NULL::varchar(300),
            'Unidad de estado sólido portátil ultracompacta con velocidades de lectura de hasta 1050 MB/s e interfaz USB-C / USB-A incluida.',
            TRUE
        ),
        (
            'Power Bank Anker 20000mAh Carga Rápida 22.5W',
            'Accesorios',
            189.00,
            19,
            0,
            NULL::varchar(300),
            'Batería externa de alta capacidad con puerto bidireccional USB-C, doble salida USB-A y protección térmica ActiveShield.',
            TRUE
        ),
        (
            'Mochila Targus Intellect Essentials 15.6 Pulgadas',
            'Accesorios',
            99.00,
            28,
            0,
            NULL::varchar(300),
            'Mochila ligera repelente al agua con compartimento acolchado para laptops de hasta 15.6 pulgadas, correas ajustables y bolsillo frontal.',
            TRUE
        )
) AS v(nombre, categoria, precio, stock, vendidos, imagen, descripcion, activo)
WHERE NOT EXISTS (
    SELECT 1
    FROM productos p
    WHERE p.nombre = v.nombre
);

