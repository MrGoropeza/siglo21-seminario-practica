# SGCV - Sistema de Gestión Comercial y Distribución Avícola

Prototipo operacional de escritorio desarrollado para el **Trabajo Práctico 3 de Seminario de Práctica de Informática (Universidad Siglo 21)**.

El sistema implementa el núcleo transaccional de una distribuidora mayorista de bultos avícolas: parada comercial en ruta (remitos y cobranzas multi-medio), cierre y arqueo diario con segregación de fondos, administración de cuentas corrientes con libro mayor y catálogo dinámico de productos con precios históricos inmutables.

## Arquitectura del Sistema (3 Capas)

El diseño desacoplado sigue el estándar del Proceso Unificado de Desarrollo (PUD):

- **Presentación (`com.sgcv.ui`)**: Interfaz gráfica en Java Swing modernizada con Look & Feel FlatLaf. Cuatro módulos operativos: parada en ruta, arqueo diario, cuentas corrientes y catálogo.
- **Servicios Transaccionales (`com.sgcv.service`)**: Capa de negocio con fronteras transaccionales ACID (`EntregaService`, `CobranzaService`, `ArqueoService`). Orquesta validaciones de stock, control de riesgo crediticio, congelamiento de precios y persistencia coordinada.
- **Acceso a Datos (`com.sgcv.dao` y `com.sgcv.util`)**: Patrón DAO con contratos de interfaz, sentencias parametrizadas (`PreparedStatement`) sobre JDBC, `try-with-resources` y gestión centralizada de conexiones en `DatabaseConnection`.
- **Persistencia Relacional (`db/`)**: Base de datos MySQL 8.0+ bajo el motor transaccional InnoDB, normalizada en Tercera Forma Normal (3NF).

## Requisitos del Sistema

- **Java**: OpenJDK 17 o superior.
- **Base de Datos**: MySQL Server 8.0 o superior.
- **Build / Gestor de Dependencias**: Apache Maven 3.8+.

## Configuración de Base de Datos

1. Iniciar el servicio de MySQL en `localhost:3306`.
2. Ejecutar secuencialmente los scripts de la carpeta `db/`:

   ```bash
   mysql -u root -p < db/01_schema.sql
   mysql -u root -p < db/02_data.sql
   ```

3. (Opcional) Ejecutar las consultas de auditoría y reportes:

   ```bash
   mysql -u root -p < db/03_queries.sql
   ```

4. Ajustar credenciales en `src/main/resources/db.properties` si difieren de las locales.

## Compilación y Ejecución

```bash
# 1. Limpieza y compilación del código fuente
mvn clean compile

# 2. Empaquetado del artefacto (JAR con manifiesto y Class-Path a dependencias)
mvn package

# 3. Lanzamiento del prototipo operacional
mvn exec:java -Dexec.mainClass="com.sgcv.App"
```

El objetivo `package` genera `target/sgcv-desktop-1.0.0.jar` con las dependencias declaradas por `Class-Path` en el manifiesto (`mysql-connector-j`, `flatlaf`, `protobuf-java`). Maven no las embebe dentro del JAR, por lo que la ejecución soportada es `mvn exec:java`, que resuelve el classpath completo. La invocación `java -jar` requiere copiar esas tres dependencias junto al artefacto.

La aplicación parte de un modelo de dominio con precios y costos congelados por operación, por lo que cualquier cambio de precios en el catálogo impacta únicamente en ventas futuras.

## Pruebas Automatizadas

La suite se ejecuta con JUnit 5 y requiere una instancia local de MySQL con el esquema y el seed cargados:

```bash
mvn test
```

Incluye 27 tests de dominio y algoritmos (sin base de datos), 3 tests de integración contra MySQL y 10 tests de interfaz Swing que construyen los paneles reales en el EDT y verifican navegación y recálculo de caja con `doClick()` programático. Los tests de integración crean y eliminan sus propios datos, sin modificar el seed.

## Estructura de Paquetes

```text
com.sgcv
├── App.java                     # Punto de entrada de la aplicación
├── model/                       # Entidades de dominio (13)
│   ├── Articulo.java            # Clase base abstracta
│   ├── ArticuloEstandar.java    # Precio de lista uniforme
│   ├── ArticuloPromocional.java # Bonificación por volumen
│   ├── Cliente.java
│   ├── Entrega.java
│   ├── DetalleEntrega.java
│   ├── Cobranza.java
│   ├── DetalleCobranza.java
│   ├── ArqueoCaja.java
│   ├── DetalleArqueo.java
│   ├── MovimientoCuentaCorriente.java
│   ├── ParadaResult.java
│   └── ResumenDiarioDTO.java
├── dao/                         # Contratos e implementaciones JDBC (10)
│   ├── IArticuloDAO.java / ArticuloDAO.java
│   ├── IClienteDAO.java / ClienteDAO.java
│   ├── IEntregaDAO.java / EntregaDAO.java
│   ├── ICobranzaDAO.java / CobranzaDAO.java
│   └── IArqueoDAO.java / ArqueoDAO.java
├── service/                     # Contratos e implementaciones de negocio (10)
│   ├── IArticuloService.java / ArticuloService.java
│   ├── IClienteService.java / ClienteService.java
│   ├── IEntregaService.java / EntregaService.java
│   ├── ICobranzaService.java / CobranzaService.java
│   └── IArqueoService.java / ArqueoService.java
├── ui/                          # Interfaz Swing (6)
│   ├── MainFrame.java
│   ├── EntregaPanel.java
│   ├── ArqueoPanel.java
│   ├── CuentasCorrientesPanel.java
│   ├── ClienteDialog.java
│   └── ClienteAutoCompleteBox.java
├── util/                        # Infraestructura JDBC (1)
│   └── DatabaseConnection.java
└── exception/                   # Excepciones de dominio (3)
    ├── StockInsuficienteException.java
    ├── LimiteCreditoExcedidoException.java
    └── ValidacionException.java
```

## Repositorio Oficial

- **GitHub**: [https://github.com/MrGoropeza/siglo21-seminario-practica.git](https://github.com/MrGoropeza/siglo21-seminario-practica.git)
