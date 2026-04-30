package mx.gob.profeco.quejas.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import mx.gob.profeco.quejas.service.NotificacionBroadcaster;

/**
 * Recurso SSE - Notificaciones en Tiempo Real para el Ciudadano.
 *
 * *** Alineado con las diapositivas de SSE (Server-Sent Events) ***
 *
 * Endpoint:
 *   GET /api/notificaciones/{ciudadanoId}
 *       Content-Type: text/event-stream
 *
 * ¿Por qué SSE y no WebSocket?
 *   Según las diapositivas, SSE es la elección correcta cuando:
 *     - La comunicación es UNIDIRECCIONAL: solo servidor → cliente.
 *     - El cliente solo necesita recibir notificaciones (no enviar datos).
 *     - Se prefiere simplicidad sobre bidireccionalidad.
 *   En PROFECO, el ciudadano solo ESCUCHA cambios de estado; no envía
 *   mensajes al servidor desde el stream. Por eso SSE > WebSocket aquí.
 *
 * Protocolo SSE (diapositivas):
 *   - El cliente abre una conexión HTTP GET normal.
 *   - El servidor responde con Content-Type: text/event-stream.
 *   - La conexión permanece abierta (long-lived).
 *   - El servidor envía eventos con formato:
 *       event: cambio-estado\n
 *       data: {"quejaId":1,"nuevoEstado":"EN_PROCESO"}\n\n
 *   - Si se pierde la conexión, el navegador reconecta automáticamente.
 */
@Path("/api/notificaciones")
public class NotificacionSSEResource {

    @Inject
    NotificacionBroadcaster broadcaster;

    /**
     * El ciudadano abre esta conexión SSE para recibir notificaciones.
     *
     * @param ciudadanoId  ID del ciudadano que escucha sus quejas.
     * @param sink         Canal SSE hacia el cliente (inyectado por JAX-RS).
     * @param sse          Fábrica SSE para crear eventos (inyectada por JAX-RS).
     */
    @GET
    @Path("/{ciudadanoId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)   // text/event-stream
    public void suscribir(@PathParam("ciudadanoId") Long ciudadanoId,
                           @Context SseEventSink sink,
                           @Context Sse sse) {

        // Pasar la fábrica SSE al broadcaster (necesaria para crear eventos)
        broadcaster.setSse(sse);

        // Registrar el sink del cliente en el broadcaster
        broadcaster.suscribir(ciudadanoId, sink);

        // Enviar evento de bienvenida para confirmar la conexión al cliente
        try {
            sink.send(
                sse.newEventBuilder()
                   .name("conexion")
                   .data("Conectado al sistema de notificaciones PROFECO. "
                       + "Ciudadano ID: " + ciudadanoId)
                   .build()
            );
        } catch (Exception e) {
            // El cliente puede haberse desconectado antes del welcome
            broadcaster.desuscribir(ciudadanoId);
        }
    }
}
