-- ==============================================================================
-- SISTEMA DE GESTION COMERCIAL Y DISTRIBUCION AVICOLA (SGCV)
-- Script: 01_schema.sql (DDL)
-- Motor: MySQL 8.0+ / InnoDB / Charset: utf8mb4
-- Normalizacion: Tercera Forma Normal (3NF)
-- ==============================================================================

DROP DATABASE IF EXISTS sgcv_db;
CREATE DATABASE sgcv_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sgcv_db;

-- ------------------------------------------------------------------------------
-- 1. TABLA: usuarios
-- Actores con acceso al sistema (Administrador, Repartidor/Vendedor).
-- ------------------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    rol ENUM('ADMINISTRADOR', 'REPARTIDOR') NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 2. TABLA: clientes
-- Comercios minoristas (pollerias, carnicerias, rotiserias, restaurantes).
-- ------------------------------------------------------------------------------
CREATE TABLE clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    razon_social VARCHAR(120) NOT NULL,
    cuit VARCHAR(20) NOT NULL UNIQUE,
    direccion VARCHAR(200) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    zona_reparto VARCHAR(50) NOT NULL,
    saldo_cuenta_corriente DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    limite_credito DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 3. TABLA: proveedores
-- Frigorificos y plantas de faena avicola.
-- ------------------------------------------------------------------------------
CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre_comercial VARCHAR(100) NOT NULL,
    razon_social VARCHAR(120) NOT NULL,
    cuit VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(50) NOT NULL,
    saldo_deuda DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    plazo_pago_dias INT NOT NULL DEFAULT 15,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 4. TABLA: articulos
-- Bultos y cajas cerradas estandar mayoristas.
-- ------------------------------------------------------------------------------
CREATE TABLE articulos (
    id_articulo INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(120) NOT NULL,
    unidad_embalaje VARCHAR(50) NOT NULL COMMENT 'Ej: Caja x 20kg, Cajon x 15kg',
    precio_costo_reposicion DECIMAL(12,2) NOT NULL,
    precio_venta_base DECIMAL(12,2) NOT NULL,
    stock_deposito INT NOT NULL DEFAULT 0,
    stock_minimo_alerta INT NOT NULL DEFAULT 10,
    activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 5. TABLA: entregas
-- Cabecera de remitos de venta y distribucion en ruta.
-- ------------------------------------------------------------------------------
CREATE TABLE entregas (
    id_entrega INT AUTO_INCREMENT PRIMARY KEY,
    numero_remito VARCHAR(30) NOT NULL UNIQUE,
    id_cliente INT NOT NULL,
    id_usuario INT NOT NULL COMMENT 'Repartidor responsable de la entrega',
    fecha_entrega DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_entrega DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    estado ENUM('EMITIDA', 'ANULADA') NOT NULL DEFAULT 'EMITIDA',
    observaciones VARCHAR(255),
    CONSTRAINT fk_entrega_cliente FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente) ON DELETE RESTRICT,
    CONSTRAINT fk_entrega_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 6. TABLA: detalle_entregas
-- Renglones de mercaderia entregada con snapshot temporal inmutable de precios.
-- ------------------------------------------------------------------------------
CREATE TABLE detalle_entregas (
    id_detalle_entrega INT AUTO_INCREMENT PRIMARY KEY,
    id_entrega INT NOT NULL,
    id_articulo INT NOT NULL,
    cantidad_bultos INT NOT NULL,
    precio_unitario_congelado DECIMAL(12,2) NOT NULL,
    costo_unitario_congelado DECIMAL(12,2) NOT NULL,
    descuento_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_det_entrega FOREIGN KEY (id_entrega)
        REFERENCES entregas(id_entrega) ON DELETE CASCADE,
    CONSTRAINT fk_det_articulo FOREIGN KEY (id_articulo)
        REFERENCES articulos(id_articulo) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 7. TABLA: cobranzas
-- Cabecera de recibos oficiales de pago de clientes.
-- ------------------------------------------------------------------------------
CREATE TABLE cobranzas (
    id_cobranza INT AUTO_INCREMENT PRIMARY KEY,
    numero_recibo VARCHAR(30) NOT NULL UNIQUE,
    id_cliente INT NOT NULL,
    id_usuario INT NOT NULL COMMENT 'Cobrador o repartidor receptor',
    fecha_cobranza DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_total DECIMAL(12,2) NOT NULL,
    observaciones VARCHAR(255),
    CONSTRAINT fk_cobranza_cliente FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente) ON DELETE RESTRICT,
    CONSTRAINT fk_cobranza_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 8. TABLA: detalle_cobranzas
-- Desglose multi-tender (efectivo, transferencias, cheques) por cobranza.
-- ------------------------------------------------------------------------------
CREATE TABLE detalle_cobranzas (
    id_detalle_cobranza INT AUTO_INCREMENT PRIMARY KEY,
    id_cobranza INT NOT NULL,
    medio_pago ENUM('EFECTIVO', 'TRANSFERENCIA') NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    referencia_operacion VARCHAR(100) COMMENT 'Numero de comprobante bancario o lote',
    CONSTRAINT fk_det_cobranza FOREIGN KEY (id_cobranza)
        REFERENCES cobranzas(id_cobranza) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 9. TABLA: compras
-- Cabecera de remitos/facturas de abastecimiento a frigorificos.
-- ------------------------------------------------------------------------------
CREATE TABLE compras (
    id_compra INT AUTO_INCREMENT PRIMARY KEY,
    numero_remito_proveedor VARCHAR(50) NOT NULL,
    id_proveedor INT NOT NULL,
    fecha_compra DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_compra DECIMAL(12,2) NOT NULL,
    estado ENUM('CONFIRMADA', 'ANULADA') NOT NULL DEFAULT 'CONFIRMADA',
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedores(id_proveedor) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 10. TABLA: detalle_compras
-- Renglones de compra a frigorifico con snapshot de costo de reposicion.
-- ------------------------------------------------------------------------------
CREATE TABLE detalle_compras (
    id_detalle_compra INT AUTO_INCREMENT PRIMARY KEY,
    id_compra INT NOT NULL,
    id_articulo INT NOT NULL,
    cantidad_bultos INT NOT NULL,
    costo_unitario_congelado DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_det_compra FOREIGN KEY (id_compra)
        REFERENCES compras(id_compra) ON DELETE CASCADE,
    CONSTRAINT fk_det_compra_articulo FOREIGN KEY (id_articulo)
        REFERENCES articulos(id_articulo) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 11. TABLA: pagos_proveedor
-- Cabecera de ordenes de pago a frigorificos.
-- ------------------------------------------------------------------------------
CREATE TABLE pagos_proveedor (
    id_pago_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    numero_orden_pago VARCHAR(30) NOT NULL UNIQUE,
    id_proveedor INT NOT NULL,
    fecha_pago DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_total DECIMAL(12,2) NOT NULL,
    observaciones VARCHAR(255),
    CONSTRAINT fk_pago_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedores(id_proveedor) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 12. TABLA: detalle_pagos_proveedor
-- Desglose multi-tender de pagos emitidos a proveedores.
-- ------------------------------------------------------------------------------
CREATE TABLE detalle_pagos_proveedor (
    id_detalle_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_pago_proveedor INT NOT NULL,
    medio_pago ENUM('EFECTIVO', 'TRANSFERENCIA') NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    referencia_operacion VARCHAR(100),
    CONSTRAINT fk_det_pago FOREIGN KEY (id_pago_proveedor)
        REFERENCES pagos_proveedor(id_pago_proveedor) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 13. TABLA: arqueos_caja
-- Cierre de jornada diario y conciliacion fisica de caja.
-- ------------------------------------------------------------------------------
CREATE TABLE arqueos_caja (
    id_arqueo INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL COMMENT 'Operador que realiza el cierre',
    fecha_arqueo DATE NOT NULL,
    saldo_inicial_efectivo DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    efectivo_esperado DECIMAL(12,2) NOT NULL,
    efectivo_real_contado DECIMAL(12,2) NOT NULL,
    diferencia_efectivo DECIMAL(12,2) NOT NULL,
    fondo_reposicion_segregado DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    margen_neto_dia DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    observaciones TEXT,
    estado ENUM('CERRADO', 'AJUSTADO') NOT NULL DEFAULT 'CERRADO',
    fecha_cierre TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_arqueo_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 14. TABLA: detalle_arqueos
-- Consolidado diario por canal / medio de pago (libro mayor del dia).
-- ------------------------------------------------------------------------------
CREATE TABLE detalle_arqueos (
    id_detalle_arqueo INT AUTO_INCREMENT PRIMARY KEY,
    id_arqueo INT NOT NULL,
    medio_pago ENUM('EFECTIVO', 'TRANSFERENCIA') NOT NULL,
    total_cobranzas DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_pagos_proveedor DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    saldo_neto DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_det_arqueo FOREIGN KEY (id_arqueo)
        REFERENCES arqueos_caja(id_arqueo) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- INDICES DE RENDIMIENTO Y CONSULTA
-- ------------------------------------------------------------------------------
CREATE INDEX idx_entregas_cliente_fecha ON entregas(id_cliente, fecha_entrega);
CREATE INDEX idx_cobranzas_cliente_fecha ON cobranzas(id_cliente, fecha_cobranza);
CREATE INDEX idx_compras_proveedor_fecha ON compras(id_proveedor, fecha_compra);
CREATE INDEX idx_articulos_codigo ON articulos(codigo);
CREATE INDEX idx_arqueos_fecha ON arqueos_caja(fecha_arqueo);
