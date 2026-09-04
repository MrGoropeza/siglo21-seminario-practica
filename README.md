# SGCV - Sistema de Gestión de Carnes y Ventas

Sistema de gestión comercial y logística para la distribución y venta de productos cárnicos, desarrollado para la cátedra de Seminario de Práctica de Informática.

## Arquitectura del Sistema
El proyecto implementa una arquitectura en tres capas:
- **Capa de Presentación (UI)**: Formularios interactivos en Java Swing (`JFrame`, `JDialog`, `JTable`).
- **Capa de Lógica de Negocio (Service)**: Servicios de validación, reglas comerciales, gestión de pedidos, entregas, cuentas corrientes y arqueo de caja.
- **Capa de Acceso a Datos (DAO)**: Patrón Data Access Object con JDBC y transacciones ACID.
- **Persistencia**: MySQL Server 8.0+ (Motor InnoDB).

## Requisitos Previos
- **Java Development Kit (JDK)**: Versión 17 o superior.
- **Gestor de dependencias**: Apache Maven 3.8+ (o Gradle 8+).
- **Base de Datos**: MySQL Server 8.0+ con soporte para transacciones ACID.

## Estructura del Proyecto
```
sgcv-app/
├── src/
│   ├── main/
│   │   ├── java/         # Código fuente de la aplicación
│   │   └── resources/    # Configuraciones y recursos
│   └── test/
│       └── java/         # Pruebas unitarias e integración
├── pom.xml               # Configuración de compilación y dependencias
└── README.md
```
