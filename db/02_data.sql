-- ==============================================================================
-- SISTEMA DE GESTION COMERCIAL Y DISTRIBUCION AVICOLA (SGCV)
-- Script: 02_data.sql (DML - Carga Inicial / Semilla)
-- Dominio: Distribucion mayorista B2B de bultos avicolas y cuentas corrientes
-- ==============================================================================

USE sgcv_db;

-- Desactivar temporalmente chequeos para carga limpia
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE detalle_arqueos;
TRUNCATE TABLE arqueos_caja;
TRUNCATE TABLE detalle_pagos_proveedor;
TRUNCATE TABLE pagos_proveedor;
TRUNCATE TABLE detalle_compras;
TRUNCATE TABLE compras;
TRUNCATE TABLE detalle_cobranzas;
TRUNCATE TABLE cobranzas;
TRUNCATE TABLE detalle_entregas;
TRUNCATE TABLE entregas;
TRUNCATE TABLE articulos;
TRUNCATE TABLE proveedores;
TRUNCATE TABLE clientes;
TRUNCATE TABLE usuarios;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 1. USUARIOS DEL SISTEMA
-- Contraseñas hasheadas en SHA-256 ('admin123', 'repartidor123')
-- ------------------------------------------------------------------------------
INSERT INTO usuarios (id_usuario, username, password_hash, nombre_completo, rol, activo) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Gonzalo Oropeza', 'ADMINISTRADOR', TRUE),
(2, 'repartidor1', '482c811da5d5b4bc6d497ffa98491e3800722279c4f684ce2645e88132454276', 'Carlos Martinez', 'REPARTIDOR', TRUE),
(3, 'repartidor2', '482c811da5d5b4bc6d497ffa98491e3800722279c4f684ce2645e88132454276', 'Esteban Gomez', 'REPARTIDOR', TRUE);

-- ------------------------------------------------------------------------------
-- 2. CLIENTES (Canal Minorista B2B con Cuentas Corrientes)
-- ------------------------------------------------------------------------------
INSERT INTO clientes (id_cliente, razon_social, cuit, direccion, telefono, zona_reparto, saldo_cuenta_corriente, limite_credito, activo) VALUES
(1, 'Polleria San Cayetano SRL', '30-71124589-8', 'Av. Rivadavia 4520, CABA', '11-4567-8901', 'Zona Oeste', 185000.00, 300000.00, TRUE),
(2, 'Rotiseria y Carniceria El Sol', '20-28456123-4', 'Belgrano 1240, Moron', '11-4629-1122', 'Zona Oeste', 420000.00, 400000.00, TRUE), -- Límite excedido (alerta CU-01 S10)
(3, 'Granja y Comestibles Don Tito', '27-32987654-2', 'Mitre 850, Ramos Mejia', '11-4654-3344', 'Zona Norte', 0.00, 250000.00, TRUE),
(4, 'Restaurante y Asador Las Brasas', '30-68954125-9', 'Av. Santa Fe 3200, Palermo', '11-4821-5566', 'Zona Norte', 260000.00, 500000.00, TRUE),
(5, 'Polleria Los Primos', '20-35123987-1', 'San Martin 210, San Justo', '11-4484-7788', 'Zona Sur', 95000.00, 150000.00, TRUE);

-- ------------------------------------------------------------------------------
-- 3. PROVEEDORES (Frigoríficos Avícolas y Plantas de Faena)
-- ------------------------------------------------------------------------------
INSERT INTO proveedores (id_proveedor, nombre_comercial, razon_social, cuit, telefono, saldo_deuda, plazo_pago_dias, activo) VALUES
(1, 'Frigorifico Soychu', 'Frigorifico Soychu S.A.I.C.I.F.I.', '30-50284569-3', '03444-423100', 850000.00, 15, TRUE),
(2, 'Granja Tres Arroyos', 'Granja Tres Arroyos S.A.C.A.F.E.I.', '30-51684725-1', '011-4318-8000', 1200000.00, 21, TRUE),
(3, 'Criave Avicola', 'Frigorifico Criave S.A.', '30-58472159-6', '02227-430150', 350000.00, 10, TRUE);

-- ------------------------------------------------------------------------------
-- 4. ARTICULOS (Bultos Mayoristas Cerrados Estándar)
-- ------------------------------------------------------------------------------
INSERT INTO articulos (id_articulo, codigo, descripcion, unidad_embalaje, precio_costo_reposicion, precio_venta_base, stock_deposito, stock_minimo_alerta, activo) VALUES
(1, 'POLL-ENT-20', 'Pollo Entero Grado A Fresco', 'Caja x 20kg', 42000.00, 54000.00, 140, 30, TRUE),
(2, 'PATA-MUS-15', 'Pata Muslo Seleccionada', 'Cajon x 15kg', 30000.00, 39000.00, 85, 20, TRUE),
(3, 'PECH-SUP-15', 'Suprema de Pechuga Deshuesada', 'Caja x 15kg', 58000.00, 75000.00, 50, 15, TRUE),
(4, 'MILA-PEC-05', 'Milanesas de Pechuga Rebozadas', 'Bolsa x 5kg', 17000.00, 23500.00, 95, 25, TRUE),
(5, 'ALIT-POL-10', 'Alitas de Pollo Frescas', 'Caja x 10kg', 13000.00, 17500.00, 35, 10, TRUE),
(6, 'MENU-POL-10', 'Menudos Frescos de Pollo', 'Caja x 10kg', 6500.00, 9000.00, 18, 15, TRUE);

-- ------------------------------------------------------------------------------
-- 5. COMPRAS (Abastecimiento a Frigoríficos)
-- ------------------------------------------------------------------------------
INSERT INTO compras (id_compra, numero_remito_proveedor, id_proveedor, fecha_compra, total_compra, estado) VALUES
(1, 'R-0001-00045210', 1, '2026-09-18 08:30:00', 3320000.00, 'CONFIRMADA'),
(2, 'R-0003-00012890', 2, '2026-09-19 09:15:00', 2600000.00, 'CONFIRMADA');

INSERT INTO detalle_compras (id_detalle_compra, id_compra, id_articulo, cantidad_bultos, costo_unitario_congelado, subtotal) VALUES
-- Compra 1: Soychú (50 cajas pollo entero, 20 cajas supremas, 20 cajones pata-muslo)
(1, 1, 1, 50, 42000.00, 2100000.00),
(2, 1, 3, 10, 58000.00, 580000.00),
(3, 1, 2, 20, 30000.00, 640000.00),
-- Compra 2: Tres Arroyos (30 cajas supremas, 50 bolsas milanesas)
(4, 2, 3, 30, 58000.00, 1740000.00),
(5, 2, 4, 50, 17200.00, 860000.00);

-- ------------------------------------------------------------------------------
-- 6. ENTREGAS (Ventas en Ruta de Reparto)
-- ------------------------------------------------------------------------------
INSERT INTO entregas (id_entrega, numero_remito, id_cliente, id_usuario, fecha_entrega, total_entrega, estado, observaciones) VALUES
(1, 'REM-0001-00001001', 1, 2, '2026-09-20 10:15:00', 324000.00, 'EMITIDA', 'Entrega matutina habitual ruta oeste'),
(2, 'REM-0001-00001002', 2, 2, '2026-09-20 11:30:00', 270000.00, 'EMITIDA', 'Exceso limite aprobado por supervisor'),
(3, 'REM-0001-00001003', 4, 3, '2026-09-20 12:45:00', 445000.00, 'EMITIDA', 'Pedido reforzado fin de semana');

INSERT INTO detalle_entregas (id_detalle_entrega, id_entrega, id_articulo, cantidad_bultos, precio_unitario_congelado, costo_unitario_congelado, descuento_unitario, subtotal) VALUES
-- Entrega 1: San Cayetano (4 cajas pollo entero, 2 cajas supremas)
(1, 1, 1, 4, 54000.00, 42000.00, 0.00, 216000.00),
(2, 1, 3, 2, 75000.00, 58000.00, 2500.00, 108000.00),
-- Entrega 2: El Sol (5 cajas pollo entero)
(3, 2, 1, 5, 54000.00, 42000.00, 0.00, 270000.00),
-- Entrega 3: Las Brasas (5 cajas supremas, 2 cajones pata muslo)
(4, 3, 3, 5, 74000.00, 58000.00, 1000.00, 367000.00),
(5, 3, 2, 2, 39000.00, 30000.00, 0.00, 78000.00);

-- ------------------------------------------------------------------------------
-- 7. COBRANZAS (Recibos Multi-Tender)
-- ------------------------------------------------------------------------------
INSERT INTO cobranzas (id_cobranza, numero_recibo, id_cliente, id_usuario, fecha_cobranza, monto_total, observaciones) VALUES
(1, 'REC-0001-00000501', 1, 2, '2026-09-20 10:20:00', 200000.00, 'Pago a cuenta en parada comercial'),
(2, 'REC-0001-00000502', 2, 2, '2026-09-20 11:35:00', 150000.00, 'Cobro mixto efectivo + transferencia bancaria'),
(3, 'REC-0001-00000503', 5, 3, '2026-09-20 14:10:00', 95000.00, 'Cancelacion total de saldo anterior');

INSERT INTO detalle_cobranzas (id_detalle_cobranza, id_cobranza, medio_pago, monto, referencia_operacion) VALUES
-- Cobranza 1: 100% Efectivo en mano
(1, 1, 'EFECTIVO', 200000.00, 'Cobro chofer camion 01'),
-- Cobranza 2: Mixto (50.000 Efectivo + 100.000 Transferencia Bancaria)
(2, 2, 'EFECTIVO', 50000.00, 'Billetes fajados'),
(3, 2, 'TRANSFERENCIA', 100000.00, 'Bco Galicia Transf #994821'),
-- Cobranza 3: 100% Transferencia
(4, 3, 'TRANSFERENCIA', 95000.00, 'Alias MP: PRIMOS.POLLO');

-- ------------------------------------------------------------------------------
-- 8. PAGOS A PROVEEDORES (Cancelación de Deuda a Frigoríficos)
-- ------------------------------------------------------------------------------
INSERT INTO pagos_proveedor (id_pago_proveedor, numero_orden_pago, id_proveedor, fecha_pago, monto_total, observaciones) VALUES
(1, 'OP-0001-00000210', 1, '2026-09-20 16:00:00', 500000.00, 'Pago parcial semanal frigorífico Soychú');

INSERT INTO detalle_pagos_proveedor (id_detalle_pago, id_pago_proveedor, medio_pago, monto, referencia_operacion) VALUES
(1, 1, 'TRANSFERENCIA', 400000.00, 'Bco Santander CBU 0720111222333444555666'),
(2, 1, 'EFECTIVO', 100000.00, 'Retiro tesorería de planta');

-- ------------------------------------------------------------------------------
-- 9. ARQUEO DE CAJA DIARIO (Cierre de Jornada y Conciliación Física)
-- ------------------------------------------------------------------------------
INSERT INTO arqueos_caja (id_arqueo, id_usuario, fecha_arqueo, saldo_inicial_efectivo, efectivo_esperado, efectivo_real_contado, diferencia_efectivo, fondo_reposicion_segregado, margen_neto_dia, observaciones, estado) VALUES
(1, 1, '2026-09-20', 50000.00, 200000.00, 200000.00, 0.00, 150000.00, 126000.00, 'Cierre sin desvios. Fondo segregado reservado para faena manana.', 'CERRADO');

INSERT INTO detalle_arqueos (id_detalle_arqueo, id_arqueo, medio_pago, total_cobranzas, total_pagos_proveedor, saldo_neto) VALUES
(1, 1, 'EFECTIVO', 250000.00, 100000.00, 150000.00),
(2, 1, 'TRANSFERENCIA', 195000.00, 400000.00, -205000.00);
