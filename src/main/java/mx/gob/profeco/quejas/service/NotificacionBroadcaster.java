package mx.gob.profeco.quejas.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.Sse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broadcaster de Server-Sent Events (SSE).
 *
 * *** Alineado con las diapositivas de SSE (Server-Sent Events) ***
 *
 * Patrón Publisher/Subscriber:
 *   - Los clientes (navegadores) se SUSCRIBEN mediante un SseEventSink.
 *   - Cuando el funcionario cambia el estado de una queja, el QuejaResource
 *     llama a enviar() y este broadcaster distribuye el evento a todos
 *     los clientes suscritos para ese ciudadano.
 *
 * Características SSE (vistas en clase):
 *   - Comunicación unidireccional: Servidor → Cliente.
 *   - Mantiene la conexión HTTP abierta (long-lived).
 *   - El cliente recibe eventos con formato "data: ...\n\n".
 *   - No requiere polling; es push puro.
 *
 * Se usa un Map<Long, SseBroadcaster> para mantener un broadcaster
 * por ciudadano (clave = ciudadanoId).
 */
@ApplicationScoped
public class NotificacionBroadcaster {

    // Un broadcaster por ciudadano  (ciudadanoId -> SseBroadcaster)
    private final Map<Long, SseBroadcaster> broadcasters = new ConcurrentHashMap<>();

    private Sse sse;  // inyectado desde el Resource

    /**
     * Inicializa el contexto SSE. Llamado desde NotificacionSSEResource.
     */
    public void setSse(Sse sse) {
        this.sse = sse;
    }

    /**
     * Registra un nuevo cliente SSE (SseEventSink) para un ciudadano.
     * Se llama cuando el navegador abre GET /api/notificaciones/{ciudadanoId}.
     */
    public synchronized void suscribir(Long ciudadanoId, SseEventSink sink) {
        SseBroadcaster broadcaster = broadcasters.computeIfAbsent(
            ciudadanoId,
            id -> sse.newBroadcaster()
        );
        broadcaster.register(sink);
    }

    /**
     * Envía un evento SSE a todos los clientes suscritos para una queja.
     * Llamado por QuejaResource cuando un funcionario cambia el estado.
     *
     * El payload del evento tiene el formato JSON:
     *   { "quejaId": 1, "nuevoEstado": "EN_PROCESO" }
     *
     * @param ciudadanoId  ID del ciudadano dueño de la queja
     * @param quejaId      ID de la queja actualizada
     * @param nuevoEstado  Nuevo estado de la queja
     */
    public void enviar(Long ciudadanoId, Long quejaId, String nuevoEstado) {
        SseBroadcaster broadcaster = broadcasters.get(ciudadanoId);
        if (broadcaster == null || sse == null) return;  // nadie suscrito

        String payload = String.format(
            "{\"quejaId\":%d,\"nuevoEstado\":\"%s\"}",
            quejaId, nuevoEstado
        );

        broadcaster.broadcast(
            sse.newEventBuilder()
               .name("cambio-estado")       // nombre del evento SSE
               .data(payload)               // datos enviados al cliente
               .build()
        );
    }

    /**
     * Elimina el broadcaster de un ciudadano (cuando ya no hay conexiones activas).
     */
    public synchronized void desuscribir(Long ciudadanoId) {
        SseBroadcaster b = broadcasters.remove(ciudadanoId);
        if (b != null) b.close();
    }
}
