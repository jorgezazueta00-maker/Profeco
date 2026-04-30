# Sistema de Quejas PROFECO
### Prueba de Concepto — Sistemas Distribuidos · Unidad 2

> **Tecnologías:** REST (JAX-RS/Jakarta) · SSE (Server-Sent Events) · JDBC/SQL · Repositorio de Archivos (Java NIO) · Quarkus 3.9 · Java 17 · PostgreSQL 15

---

## Estructura del Proyecto

```
profeco-quejas/
├── pom.xml
├── README.md
├── src/
│   └── main/
│       ├── java/mx/gob/profeco/quejas/
│       │   ├── model/
│       │   │   ├── Queja.java
│       │   │   ├── QuejaDTO.java
│       │   │   ├── EstadoDTO.java
│       │   │   └── Evidencia.java
│       │   ├── repository/
│       │   │   ├── QuejaRepository.java       ← JDBC / PreparedStatement
│       │   │   └── EvidenciaRepository.java   ← JDBC + Java NIO
│       │   ├── service/
│       │   │   └── NotificacionBroadcaster.java ← SSE Pub/Sub
│       │   └── resource/
│       │       ├── QuejaResource.java         ← REST /api/quejas
│       │       ├── NotificacionSSEResource.java ← SSE /api/notificaciones
│       │       └── EvidenciaResource.java     ← REST /api/evidencias
│       └── resources/
│           ├── application.properties
│           └── schema.sql
├── uml/
│   ├── componentes.puml
│   └── despliegue.puml
└── docs/
    └── avance_u2.md
```

---

## Requisitos

- Java JDK 17+
- Maven 3.9+
- PostgreSQL 15+

---

## Paso 1 — Configurar PostgreSQL

```bash
psql -U postgres -c "CREATE DATABASE profeco_db;"
psql -U postgres -c "CREATE USER profeco_user WITH PASSWORD 'profeco_pass';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE profeco_db TO profeco_user;"
psql -U postgres -d profeco_db -c "GRANT ALL ON SCHEMA public TO profeco_user;"
psql -U profeco_user -d profeco_db -f src/main/resources/schema.sql
```

---

## Paso 2 — Ejecutar

```bash
mvn quarkus:dev
```

Servidor en: **http://localhost:8080**

---

## Paso 3 — Probar con Postman

### Registrar Queja — POST /api/quejas
```
POST http://localhost:8080/api/quejas
Content-Type: application/json

{
  "ciudadanoId": 1,
  "descripcion": "Producto defectuoso, la tienda se niega a devolverlo.",
  "empresa": "Tienda XYZ"
}
```
Respuesta esperada: **201 Created**

---

### Consultar Queja — GET /api/quejas/{id}
```
GET http://localhost:8080/api/quejas/1
```
Respuesta esperada: **200 OK**

---

### Actualizar Estado (Funcionario) — PUT /api/quejas/{id}/estado
```
PUT http://localhost:8080/api/quejas/1/estado
Content-Type: application/json

{
  "estado": "EN_PROCESO",
  "comentario": "Asignado al departamento de mediación."
}
```
Estados válidos: `EN_PROCESO`, `RESUELTA`, `CERRADA`

**Este endpoint dispara automáticamente un evento SSE al ciudadano.**

---

### Subir Evidencia — POST /api/evidencias/{quejaId}
```
POST http://localhost:8080/api/evidencias/1
Content-Type: application/octet-stream
X-Nombre-Archivo: ticket.pdf
X-Tipo-Mime: application/pdf

[Body: archivo binario]
```

---

## Paso 4 — Probar SSE en el navegador

Abrir la consola del navegador (F12) y ejecutar:

```javascript
const es = new EventSource('http://localhost:8080/api/notificaciones/1');
es.addEventListener('conexion', e => console.log('Conectado:', e.data));
es.addEventListener('cambio-estado', e => console.log('Notificación:', e.data));
```

Luego ejecutar el `PUT /api/quejas/1/estado` desde Postman y observar el evento llegando en la consola en tiempo real.

---

## Paso 5 — Probar SSE con curl

```bash
curl -N -H "Accept: text/event-stream" http://localhost:8080/api/notificaciones/1
```
