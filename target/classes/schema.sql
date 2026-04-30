-- =============================================================
--  PROFECO - Sistema de Quejas
--  schema.sql  -  Compatible con PostgreSQL 15
--  Ejecutar una vez sobre la BD profeco_db:
--    psql -U profeco_user -d profeco_db -f schema.sql
-- =============================================================

CREATE TABLE IF NOT EXISTS ciudadanos (
    id         BIGSERIAL    PRIMARY KEY,
    nombre     VARCHAR(120) NOT NULL,
    email      VARCHAR(100) NOT NULL,
    telefono   VARCHAR(20),
    fecha_alta TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quejas (
    id                  BIGSERIAL     PRIMARY KEY,
    ciudadano_id        BIGINT        NOT NULL,
    descripcion         VARCHAR(2000) NOT NULL,
    empresa             VARCHAR(200),
    estado              VARCHAR(30)   NOT NULL DEFAULT 'REGISTRADA',
    fecha_registro      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evidencias (
    id             BIGSERIAL    PRIMARY KEY,
    queja_id       BIGINT       NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL,
    tipo_mime      VARCHAR(100),
    fecha_subida   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historial_estados (
    id              BIGSERIAL   PRIMARY KEY,
    queja_id        BIGINT      NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo    VARCHAR(30) NOT NULL,
    comentario      VARCHAR(500),
    fecha_cambio    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- Datos de prueba (solo si la tabla está vacía)
INSERT INTO ciudadanos (id, nombre, email, telefono)
    SELECT 1, 'Juan Perez Lopez', 'juan.perez@ejemplo.com', '5551234567'
    WHERE NOT EXISTS (SELECT 1 FROM ciudadanos WHERE id = 1);

INSERT INTO ciudadanos (id, nombre, email, telefono)
    SELECT 2, 'Maria Garcia Ruiz', 'maria.garcia@ejemplo.com', '5559876543'
    WHERE NOT EXISTS (SELECT 1 FROM ciudadanos WHERE id = 2);
