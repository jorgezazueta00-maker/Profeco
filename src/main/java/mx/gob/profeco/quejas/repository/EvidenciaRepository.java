package mx.gob.profeco.quejas.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mx.gob.profeco.quejas.model.Evidencia;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de Evidencias.
 *
 * *** Alineado con diapositivas de Repositorio de Archivos y JDBC ***
 *
 * 1. Guarda el ARCHIVO BINARIO en disco con Java NIO (Files.copy).
 * 2. Guarda los METADATOS en la BD H2 con JDBC / PreparedStatement.
 *
 * Compatible con H2 embebida (sin RETURNING, sin sintaxis PostgreSQL).
 */
@ApplicationScoped
public class EvidenciaRepository {

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "profeco.evidencias.directorio", defaultValue = "./evidencias")
    String directorioBase;

    // ------------------------------------------------------------------
    // GUARDAR archivo + metadatos
    // ------------------------------------------------------------------
    public Evidencia guardar(Long quejaId, String nombreOriginal,
                              String tipoMime, InputStream contenido)
            throws IOException, SQLException {

        // 1. Crear subdirectorio por queja (Java NIO)
        Path dirQueja = Paths.get(directorioBase, "queja_" + quejaId);
        Files.createDirectories(dirQueja);

        // 2. Nombre único para evitar colisiones
        String extension = obtenerExtension(nombreOriginal);
        String nombreUnico = UUID.randomUUID().toString() + extension;
        Path rutaArchivo = dirQueja.resolve(nombreUnico);

        // 3. Copiar bytes al disco (Java NIO)
        Files.copy(contenido, rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

        // 4. Persistir metadatos en BD (JDBC)
        return guardarMetadatos(quejaId, nombreOriginal,
                                rutaArchivo.toString(), tipoMime);
    }

    // ------------------------------------------------------------------
    // LISTAR evidencias de una queja
    // ------------------------------------------------------------------
    public List<Evidencia> listarPorQueja(Long quejaId) throws SQLException {

        String sql = "SELECT id, queja_id, nombre_archivo, ruta_archivo, "
                   + "tipo_mime, fecha_subida FROM evidencias WHERE queja_id = ?";

        List<Evidencia> lista = new ArrayList<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, quejaId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Evidencia e = new Evidencia(
                    rs.getLong("queja_id"),
                    rs.getString("nombre_archivo"),
                    rs.getString("ruta_archivo"),
                    rs.getString("tipo_mime")
                );
                e.setId(rs.getLong("id"));
                Timestamp ts = rs.getTimestamp("fecha_subida");
                if (ts != null) e.setFechaSubida(ts.toLocalDateTime());
                lista.add(e);
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // DESCARGAR archivo por ID
    // ------------------------------------------------------------------
    public byte[] descargar(Long evidenciaId) throws IOException, SQLException {

        String sql = "SELECT ruta_archivo FROM evidencias WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, evidenciaId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Path ruta = Paths.get(rs.getString("ruta_archivo"));
            return Files.readAllBytes(ruta);  // Java NIO
        }
    }

    // ------------------------------------------------------------------
    // Auxiliar: guardar metadatos en BD (sin RETURNING, compatible H2)
    // ------------------------------------------------------------------
    private Evidencia guardarMetadatos(Long quejaId, String nombreArchivo,
                                        String ruta, String tipoMime)
            throws SQLException {

        String sql = "INSERT INTO evidencias(queja_id, nombre_archivo, "
                   + "ruta_archivo, tipo_mime) VALUES(?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, quejaId);
            ps.setString(2, nombreArchivo);
            ps.setString(3, ruta);
            ps.setString(4, tipoMime);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            long id = keys.getLong(1);

            Evidencia e = new Evidencia(quejaId, nombreArchivo, ruta, tipoMime);
            e.setId(id);
            return e;
        }
    }

    private String obtenerExtension(String nombre) {
        int idx = nombre.lastIndexOf('.');
        return idx >= 0 ? nombre.substring(idx) : "";
    }
}
