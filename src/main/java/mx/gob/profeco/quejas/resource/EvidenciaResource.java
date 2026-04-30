package mx.gob.profeco.quejas.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import mx.gob.profeco.quejas.model.Evidencia;
import mx.gob.profeco.quejas.repository.EvidenciaRepository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Recurso REST - Gestión de Evidencias (archivos).
 *
 * *** Alineado con las diapositivas de REST y Repositorio de Archivos ***
 *
 * Endpoints:
 *   POST  /api/evidencias/{quejaId}       → Subir archivo de evidencia
 *   GET   /api/evidencias/queja/{quejaId} → Listar metadatos de evidencias
 *   GET   /api/evidencias/{id}/descargar  → Descargar archivo por ID
 *
 * El recurso delega la lógica de persistencia al EvidenciaRepository,
 * que usa JDBC para metadatos y Java NIO para el archivo binario.
 *
 * Nota: En una implementación real se usaría multipart/form-data
 * (@MultipartForm). Para simplificar la PoC, se recibe el archivo
 * como application/octet-stream con el nombre en un header.
 */
@Path("/api/evidencias")
@Produces(MediaType.APPLICATION_JSON)
public class EvidenciaResource {

    @Inject
    EvidenciaRepository evidenciaRepository;

    // ================================================================
    // POST /api/evidencias/{quejaId}  →  Subir archivo de evidencia
    // ================================================================
    /**
     * Recibe un archivo binario y lo guarda en el repositorio de archivos.
     * Los metadatos se persisten en la BD SQL.
     *
     * Headers requeridos:
     *   Content-Type: application/octet-stream
     *   X-Nombre-Archivo: nombre_del_archivo.pdf
     *   X-Tipo-Mime: application/pdf
     */
    @POST
    @Path("/{quejaId}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response subirEvidencia(@PathParam("quejaId") Long quejaId,
                                    @HeaderParam("X-Nombre-Archivo") String nombreArchivo,
                                    @HeaderParam("X-Tipo-Mime") String tipoMime,
                                    InputStream contenido) {

        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Header X-Nombre-Archivo es obligatorio\"}")
                    .build();
        }

        if (tipoMime == null || tipoMime.isBlank()) {
            tipoMime = "application/octet-stream";  // fallback
        }

        try {
            Evidencia evidencia = evidenciaRepository.guardar(
                quejaId, nombreArchivo, tipoMime, contenido
            );
            return Response
                    .status(Response.Status.CREATED)
                    .entity(evidencia)
                    .build();

        } catch (IOException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"Error al guardar el archivo: " + e.getMessage() + "\"}")
                    .build();
        } catch (SQLException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"Error de base de datos: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ================================================================
    // GET /api/evidencias/queja/{quejaId}  →  Listar metadatos
    // ================================================================
    /**
     * Retorna la lista de evidencias (solo metadatos, sin el binario)
     * asociadas a una queja.
     */
    @GET
    @Path("/queja/{quejaId}")
    public Response listarEvidencias(@PathParam("quejaId") Long quejaId) {

        try {
            List<Evidencia> lista = evidenciaRepository.listarPorQueja(quejaId);
            return Response.ok(lista).build();

        } catch (SQLException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ================================================================
    // GET /api/evidencias/{id}/descargar  →  Descargar archivo
    // ================================================================
    /**
     * Descarga el archivo binario de una evidencia dado su ID.
     * Retorna el archivo como application/octet-stream.
     */
    @GET
    @Path("/{id}/descargar")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response descargarEvidencia(@PathParam("id") Long id) {

        try {
            byte[] bytes = evidenciaRepository.descargar(id);

            if (bytes == null) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Evidencia no encontrada\"}")
                        .build();
            }

            return Response
                    .ok(bytes)
                    .header("Content-Disposition",
                            "attachment; filename=\"evidencia_" + id + "\"")
                    .build();

        } catch (IOException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"Error al leer el archivo: " + e.getMessage() + "\"}")
                    .build();
        } catch (SQLException e) {
            return Response
                    .serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
