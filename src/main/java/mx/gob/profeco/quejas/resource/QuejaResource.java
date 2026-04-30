package mx.gob.profeco.quejas.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import mx.gob.profeco.quejas.model.EstadoDTO;
import mx.gob.profeco.quejas.model.Queja;
import mx.gob.profeco.quejas.model.QuejaDTO;
import mx.gob.profeco.quejas.repository.QuejaRepository;
import mx.gob.profeco.quejas.service.NotificacionBroadcaster;

import java.sql.SQLException;
import java.util.List;

/**
 * Recurso REST - Servicio de Quejas PROFECO.
 *
 * *** Alineado con las diapositivas de REST (JAX-RS / Jakarta REST) ***
 *
 * Endpoints expuestos:
 *   POST   /api/quejas                  → Registrar nueva queja          (ciudadano)
 *   GET    /api/quejas/{id}             → Consultar queja por ID         (ciudadano / funcionario)
 *   GET    /api/quejas/ciudadano/{cid}  → Listar quejas de un ciudadano  (ciudadano)
 *   PUT    /api/quejas/{id}/estado      → Actualizar estado de queja     (funcionario)
 *
 * Principios REST aplicados (diapositivas):
 *   - Recursos identificados por URI (/api/quejas, /api/quejas/{id})
 *   - Verbos HTTP semánticos: POST, GET, PUT
 *   - Respuestas con códigos HTTP correctos: 201, 200, 400, 404, 500
 *   - Representación en JSON (Content-Type: application/json)
 *   - Sin estado en el servidor (stateless)
 */
@Path("/api/quejas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuejaResource {

    @Inject
    QuejaRepository quejaRepository;

    @Inject
    NotificacionBroadcaster broadcaster;

    // ================================================================
    // POST /api/quejas  →  Registrar nueva queja
    // ================================================================
    /**
     * Registra una nueva queja en el sistema.
     * El ciudadano envía un JSON con ciudadanoId, descripcion y empresa.
     * Retorna 201 CREATED con el objeto Queja creado (incluyendo su ID).
     */
    @POST
    public Response registrarQueja(QuejaDTO dto) {

        // Validación básica (lado servidor)
        if (dto == null
                || dto.getCiudadanoId() == null
                || dto.getDescripcion() == null
                || dto.getDescripcion().isBlank()) {

            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"ciudadanoId y descripcion son obligatorios\"}")
                    .build();
        }

        try {
            Queja queja = quejaRepository.guardar(dto);
            return Response
                    .status(Response.Status.CREATED)  // 201
                    .entity(queja)
                    .build();

        } catch (SQLException e) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ================================================================
    // GET /api/quejas/{id}  →  Consultar queja por ID
    // ================================================================
    /**
     * Retorna los datos de una queja dado su ID.
     * 200 OK si existe; 404 NOT FOUND si no.
     */
    @GET
    @Path("/{id}")
    public Response consultarQueja(@PathParam("id") Long id) {

        try {
            Queja queja = quejaRepository.buscarPorId(id);

            if (queja == null) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Queja no encontrada\"}")
                        .build();
            }

            return Response.ok(queja).build();  // 200 OK

        } catch (SQLException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ================================================================
    // GET /api/quejas/ciudadano/{ciudadanoId}  →  Listar por ciudadano
    // ================================================================
    /**
     * Lista todas las quejas de un ciudadano específico.
     * Retorna un arreglo JSON (puede ser vacío []).
     */
    @GET
    @Path("/ciudadano/{ciudadanoId}")
    public Response listarPorCiudadano(@PathParam("ciudadanoId") Long ciudadanoId) {

        try {
            List<Queja> lista = quejaRepository.listarPorCiudadano(ciudadanoId);
            return Response.ok(lista).build();

        } catch (SQLException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ================================================================
    // PUT /api/quejas/{id}/estado  →  Actualizar estado (funcionario)
    // ================================================================
    /**
     * El funcionario actualiza el estado de una queja.
     * Tras actualizar la BD, se dispara el evento SSE al ciudadano.
     *
     * Flujo:
     *   1. Validar el cuerpo JSON recibido.
     *   2. Obtener ciudadanoId de la queja (necesario para el broadcaster SSE).
     *   3. Actualizar estado en BD via JDBC.
     *   4. Enviar evento SSE al ciudadano mediante NotificacionBroadcaster.
     *   5. Retornar 200 OK.
     */
    @PUT
    @Path("/{id}/estado")
    public Response actualizarEstado(@PathParam("id") Long id,
                                      EstadoDTO estadoDTO) {

        if (estadoDTO == null || estadoDTO.getEstado() == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"El campo estado es obligatorio\"}")
                    .build();
        }

        // Estados válidos
        String estado = estadoDTO.getEstado().toUpperCase();
        if (!List.of("EN_PROCESO", "RESUELTA", "CERRADA").contains(estado)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Estado inválido. Use: EN_PROCESO, RESUELTA, CERRADA\"}")
                    .build();
        }

        try {
            // Obtener la queja para saber el ciudadanoId (necesario para SSE)
            Queja queja = quejaRepository.buscarPorId(id);
            if (queja == null) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Queja no encontrada\"}")
                        .build();
            }

            // Actualizar en BD (con historial)
            quejaRepository.actualizarEstado(id, estado, estadoDTO.getComentario());

            // Notificar al ciudadano via SSE  ← integración REST + SSE
            broadcaster.enviar(queja.getCiudadanoId(), id, estado);

            return Response.ok("{\"mensaje\":\"Estado actualizado a " + estado + "\"}").build();

        } catch (SQLException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
