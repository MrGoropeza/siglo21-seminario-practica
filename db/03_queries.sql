-- ==============================================================================
-- SISTEMA DE GESTION COMERCIAL Y DISTRIBUCION AVICOLA (SGCV)
-- Script: 03_queries.sql (Consultas SQL Analiticas, Justificaciones y DML)
-- Motor: MySQL 8.0+ / InnoDB
-- ==============================================================================

USE sgcv_db;

-- ==============================================================================
-- 1. CONSULTAS DE SELECCION Y REPORTES ANALITICOS (SELECT)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- CONSULTA 1: Estado de Cuenta Corriente y Semaforo de Riesgo Crediticio
-- Justificación Técnica: Evalúa la salud crediticia de la cartera de clientes.
-- Combina saldos actuales con límites autorizados mediante un CASE condicional
-- para emitir alertas operativas antes de autorizar salidas en ruta de reparto.
-- ------------------------------------------------------------------------------
SELECT 
    c.id_cliente,
    c.razon_social,
    c.cuit,
    c.zona_reparto,
    c.saldo_cuenta_corriente,
    c.limite_credito,
    ROUND((c.saldo_cuenta_corriente / NULLIF(c.limite_credito, 0)) * 100, 2) AS porcentaje_utilizacion_credito,
    CASE 
        WHEN c.saldo_cuenta_corriente > c.limite_credito THEN 'EXCEDIDO - BLOQUEAR / CONFIRMACION REQUERIDA'
        WHEN (c.saldo_cuenta_corriente / c.limite_credito) >= 0.80 THEN 'ALERTA - PROXIMO AL LIMITE'
        ELSE 'NORMAL'
    END AS estado_crediticio
FROM clientes c
WHERE c.activo = TRUE
ORDER BY c.saldo_cuenta_corriente DESC;

-- ------------------------------------------------------------------------------
-- CONSULTA 2: Rentabilidad y Margen Bruto Real por Articulo (Snapshot Inmutable)
-- Justificación Técnica: Utiliza los precios y costos unitarios congelados en el
-- detalle de entregas para calcular el margen comercial real obtenido, inmune
-- a fluctuaciones o aumentos de listas de precios posteriores a la venta.
-- ------------------------------------------------------------------------------
SELECT 
    a.codigo,
    a.descripcion,
    a.unidad_embalaje,
    SUM(de.cantidad_bultos) AS total_bultos_entregados,
    SUM(de.subtotal) AS facturacion_total,
    SUM(de.cantidad_bultos * de.costo_unitario_congelado) AS costo_mercaderia_vendida_total,
    SUM(de.subtotal - (de.cantidad_bultos * de.costo_unitario_congelado)) AS margen_bruto_pesos,
    ROUND(
        (SUM(de.subtotal - (de.cantidad_bultos * de.costo_unitario_congelado)) / NULLIF(SUM(de.subtotal), 0)) * 100, 
        2
    ) AS porcentaje_margen_bruto
FROM detalle_entregas de
INNER JOIN entregas e ON de.id_entrega = e.id_entrega
INNER JOIN articulos a ON de.id_articulo = a.id_articulo
WHERE e.estado = 'EMITIDA'
GROUP BY a.id_articulo, a.codigo, a.descripcion, a.unidad_embalaje
ORDER BY margen_bruto_pesos DESC;

-- ------------------------------------------------------------------------------
-- CONSULTA 3: Auditoria Diaria de Cobranzas por Medio de Pago y Repartidor
-- Justificación Técnica: Desglosa los fondos recaudados en ruta cruzando la cabecera
-- del recibo con su detalle multi-tender para alimentar el arqueo de caja físico.
-- ------------------------------------------------------------------------------
SELECT 
    DATE(c.fecha_cobranza) AS fecha,
    u.nombre_completo AS cobrador_repartidor,
    dc.medio_pago,
    COUNT(DISTINCT c.id_cobranza) AS cantidad_recibos,
    SUM(dc.monto) AS subtotal_recaudado
FROM cobranzas c
INNER JOIN detalle_cobranzas dc ON c.id_cobranza = dc.id_cobranza
INNER JOIN usuarios u ON c.id_usuario = u.id_usuario
GROUP BY DATE(c.fecha_cobranza), u.id_usuario, u.nombre_completo, dc.medio_pago
ORDER BY fecha DESC, u.nombre_completo, dc.medio_pago;

-- ------------------------------------------------------------------------------
-- CONSULTA 4: Control de Inventario y Alerta de Quiebre de Stock
-- Justificación Técnica: Compara el stock físico actual en depósito frente al
-- punto de reposición mínimo configurado, calculando los bultos a pedir.
-- ------------------------------------------------------------------------------
SELECT 
    a.codigo,
    a.descripcion,
    a.unidad_embalaje,
    a.stock_deposito,
    a.stock_minimo_alerta,
    (a.stock_minimo_alerta - a.stock_deposito) AS bultos_a_reponer,
    a.precio_costo_reposicion,
    ((a.stock_minimo_alerta - a.stock_deposito) * a.precio_costo_reposicion) AS capital_reposicion_estimado
FROM articulos a
WHERE a.stock_deposito <= a.stock_minimo_alerta AND a.activo = TRUE
ORDER BY (a.stock_deposito - a.stock_minimo_alerta) ASC;

-- ------------------------------------------------------------------------------
-- CONSULTA 5: Resumen Consolidado de Deuda a Frigorificos Proveedores
-- Justificación Técnica: Expone los compromisos pendientes de pago con plantas
-- de faena avícola para programar transferencias y proteger el canal de compra.
-- ------------------------------------------------------------------------------
SELECT 
    p.id_proveedor,
    p.nombre_comercial,
    p.cuit,
    p.plazo_pago_dias,
    p.saldo_deuda,
    COUNT(c.id_compra) AS total_compras_historicas,
    COALESCE(MAX(c.fecha_compra), 'Sin compras registradas') AS fecha_ultima_compra
FROM proveedores p
LEFT JOIN compras c ON p.id_proveedor = c.id_proveedor AND c.estado = 'CONFIRMADA'
WHERE p.activo = TRUE
GROUP BY p.id_proveedor, p.nombre_comercial, p.cuit, p.plazo_pago_dias, p.saldo_deuda
ORDER BY p.saldo_deuda DESC;

-- ------------------------------------------------------------------------------
-- CONSULTA 6: Resumen Ejecutivo de Arqueo Diario con Auditoria de Desvios
-- Justificación Técnica: Audita el balance de cierre diario de caja verificando
-- la exactitud entre el efectivo teórico y el recuento físico del operador.
-- ------------------------------------------------------------------------------
SELECT 
    ac.id_arqueo,
    ac.fecha_arqueo,
    u.nombre_completo AS responsable_cierre,
    ac.saldo_inicial_efectivo,
    ac.efectivo_esperado,
    ac.efectivo_real_contado,
    ac.diferencia_efectivo,
    ac.fondo_reposicion_segregado,
    ac.margen_neto_dia,
    ac.estado,
    CASE 
        WHEN ac.diferencia_efectivo = 0 THEN 'CONCILIACION EXACTA'
        WHEN ac.diferencia_efectivo < 0 THEN CONCAT('FALTANTE DE CAJA: $', ABS(ac.diferencia_efectivo))
        ELSE CONCAT('SOBRANTE DE CAJA: $', ac.diferencia_efectivo)
    END AS diagnostico_auditoria
FROM arqueos_caja ac
INNER JOIN usuarios u ON ac.id_usuario = u.id_usuario
ORDER BY ac.fecha_arqueo DESC;


-- ==============================================================================
-- 2. OPERACIONES DE ACTUALIZACION CONTROLADA (UPDATE)
-- ==============================================================================

-- Actualización atómica de saldo tras imputación de cobranza
UPDATE clientes 
SET saldo_cuenta_corriente = saldo_cuenta_corriente - 200000.00
WHERE id_cliente = 1;

-- Actualización de costo de reposición y precio de venta base de un bulto
UPDATE articulos
SET precio_costo_reposicion = 45000.00,
    precio_venta_base = 58000.00
WHERE codigo = 'POLL-ENT-20';


-- ==============================================================================
-- 3. OPERACIONES DE BORRADO CONTROLADO Y BAJA LOGICA (DELETE / SOFT-DELETE)
-- ==============================================================================

-- Baja lógica de cliente para preservar integridad referencial de remitos y recibos
UPDATE clientes 
SET activo = FALSE 
WHERE id_cliente = 3;

-- Borrado físico protegido por FOREIGN KEY RESTRICT:
-- Intentar borrar un artículo asociado a un detalle de entrega fallará con error 1451
-- DELETE FROM articulos WHERE id_articulo = 1; -- (Bloqueado por ON DELETE RESTRICT)

-- Borrado controlado en tabla transaccional de detalle mediante CASCADE
-- DELETE FROM cobranzas WHERE id_cobranza = 999; -- (Elimina en cascada detalle_cobranzas)
