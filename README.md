# SGCV - Sistema de Gestión Comercial y Distribución Avícola

Prototipo operacional de escritorio desarrollado para el **Trabajo Práctico 2 de Seminario de Práctica de Informática (Universidad Siglo 21)**.

El sistema implementa el núcleo transaccional para una empresa distribuidora mayorista de bultos avícolas estándar (cajas cerradas), con soporte para cuentas corrientes en ruta, control de límites crediticios, inventario en depósito y congelamiento inmutable de precios históricos.

## Arquitectura del Sistema (3 Capas)

El diseño desacoplado sigue el estándar del Proceso Unificado de Desarrollo (PUD):
- **Presentación (`com.sgcv.ui`)**: Interfaz gráfica en Java Swing modernizada con Look & Feel FlatLaf. Formularios modulares con validación interactiva de crédito y selección de bultos.
- **Servicios Transaccionales (`com.sgcv.service`)**: Capa de negocio con delimitación de fronteras transaccionales ACID (`EntregaService`). Orquesta validaciones de stock, advertencia de sobregiro crediticio y persistencia coordinada.
- **Acceso a Datos (`com.sgcv.dao` y `com.sgcv.util`)**: Patrón DAO desacoplado mediante sentencias parametrizadas (`PreparedStatement`) sobre JDBC y gestión centralizada de conexiones con `DatabaseConnection`.
- **Persistencia Relacional (`db/`)**: Base de datos MySQL 8.0+ bajo el motor transaccional InnoDB, normalizada en Tercera Forma Normal (3NF).

## Requisitos del Sistema
- **Java**: OpenJDK 17 o superior.
- **Base de Datos**: MySQL Server 8.0 o superior.
- **Build / Gestor de Dependencias**: Apache Maven 3.8+ (compatible con IntelliJ IDEA, NetBeans y Eclipse).

## Configuración de Base de Datos

1. Iniciar el servicio de MySQL en `localhost:3306`.
2. Ejecutar secuencialmente los scripts ubicados en la carpeta `db/`:
   ```bash
   mysql -u root -p < db/01_schema.sql
   mysql -u root -p < db/02_data.sql
   ```
3. (Opcional) Ejecutar las consultas de auditoría y reportes:
   ```bash
   mysql -u root -p < db/03_queries.sql
   ```
4. Ajustar credenciales en `src/main/resources/db.properties` si difieren de `root / root`.

## Compilación y Ejecución

### Mediante Maven
```bash
# Compilar el proyecto y empaquetar el JAR
mvn clean package

# Ejecutar el prototipo operacional
java -jar target/sgcv-desktop-1.0.0.jar
```

### Mediante compilador directo (javac)
```bash
# Compilar fuentes
javac -d target/classes $(find src/main/java -name "*.java")

# Copiar archivo de configuración
cp src/main/resources/db.properties target/classes/

# Ejecutar la aplicación
java -cp target/classes com.sgcv.App
```

## Estructura de Paquetes
```
com.sgcv
├── App.java                   # Punto de entrada de la aplicación
├── model/                     # Entidades de dominio
│   ├── Cliente.java
│   ├── Articulo.java
│   ├── Entrega.java
│   └── DetalleEntrega.java
├── dao/                       # Capa de persistencia JDBC
│   ├── ClienteDAO.java
│   ├── ArticuloDAO.java
│   └── EntregaDAO.java
├── service/                   # Capa de servicios transaccionales
│   └── EntregaService.java
├── ui/                        # Interfaz gráfica de usuario Swing
│   ├── MainFrame.java
│   └── EntregaPanel.java
└── util/                      # Clases utilitarias
    └── DatabaseConnection.java
```

## Repositorio Oficial
- **GitHub**: [https://github.com/MrGoropeza/siglo21-seminario-practica.git](https://github.com/MrGoropeza/siglo21-seminario-practica.git)
