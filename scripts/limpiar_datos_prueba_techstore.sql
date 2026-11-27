-- PostgreSQL / TechStore. SOLO para una base de datos de pruebas.
-- Detener backend y hacer copia de seguridad antes de ejecutar.
-- RESTRICT impide truncar si hay tablas dependientes no incluidas.
BEGIN;
DO $$
DECLARE
    tablas text;
BEGIN
    IF to_regclass('public.productos') IS NULL OR
       to_regclass('public.pedidos') IS NULL OR
       to_regclass('public.entradas') IS NULL THEN
        RAISE EXCEPTION 'No se encontró el esquema esperado de TechStore';
    END IF;
    SELECT string_agg(format('public.%I', nombre), ', ')
      INTO tablas
      FROM unnest(ARRAY['comprobantes_pedido','detalle_pedidos','pedidos',
          'despachos_lotes','detalle_entradas','entradas',
          'especificaciones_tecnicas','inventarios_iniciales','productos']) AS t(nombre)
      WHERE to_regclass(format('public.%I', nombre)) IS NOT NULL;
    EXECUTE 'TRUNCATE TABLE ' || tablas || ' RESTART IDENTITY RESTRICT';
END $$;
SELECT (SELECT count(*) FROM public.productos) AS productos,
       (SELECT count(*) FROM public.pedidos) AS pedidos,
       (SELECT count(*) FROM public.entradas) AS entradas;
COMMIT;
-- No elimina usuarios, proveedores, roles, permisos ni catálogos geográficos/personales.
