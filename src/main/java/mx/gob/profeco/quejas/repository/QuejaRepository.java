package mx.gob.profeco.quejas.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mx.gob.profeco.quejas.model.Queja;
import mx.gob.profeco.quejas.model.QuejaDTO;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de Quejas usando JDBC puro con PreparedStatement.
 *
 * *** Alineado con las diapositivas de JDBC/SQL ***
 * - DataSource inyectado por Quarkus/Agroal (pool de conexiones).
 * - Toda consulta usa PreparedStatement (evita SQL Injection).
 * - try-with-resources para cerrar Connection, Statement y ResultSet.
 * - NO se usa ningun ORM (sin Hibernate, sin JPA).
 * - Compatible con H2 (BD embebida, sin instalacion externa).
 */
@ApplicationScoped
public class QuejaRepository {

    @Inject
    DataSource dataSource;

    // ------------------------------------------------------------------
    // INSERTAR nueva queja
    // ------------------------------------------------------------------
    public Queja guardar(QuejaDTO dto) throws SQLException {

        String sql = "INSERT INTO quejas(ciudadano_id, descripcion, empresa, estado, "
                   + "fecha_registro) VALUES(?, ?, ?, 'REGISTRADA', CURRENT_TIMESTAMP)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {  // H2: obtener ID generado

            ps.setLong(1, dto.getCiudadanoId());
            ps.setString(2, dto.getDescripcion());
            ps.setString(3, dto.getEmpresa());
            ps.executeUpdate();

            // Obtener el ID autogenerado (compatible H2 y PostgreSQL)
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            long id = keys.getLong(1);

            return new Queja(id, dto.getCiudadanoId(),
                             dto.getDescripcion(), dto.getEmpresa(), "REGISTRADA");
        }
    }

    // ------------------------------------------------------------------
    // BUSCAR queja por ID
    // ------------------------------------------------------------------
    public Queja buscarPorId(Long id) throws SQLException {

        String sql = "SELECT id, ciudadano_id, descripcion, empresa, estado, "
                   + "fecha_registro, fecha_actualizacion "
                   + "FROM quejas WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            return mapearQueja(rs);
        }
    }

    // ------------------------------------------------------------------
    // LISTAR quejas de un ciudadano
    // ------------------------------------------------------------------
    public List<Queja> listarPorCiudadano(Long ciudadanoId) throws SQLException {

        String sql = "SELECT id, ciudadano_id, descripcion, empresa, estado, "
                   + "fecha_registro, fecha_actualizacion "
                   + "FROM quejas WHERE ciudadano_id = ? ORDER BY fecha_registro DESC";

        List<Queja> lista = new ArrayList<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, ciudadanoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapearQueja(rs));
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // ACTUALIZAR estado de una queja
    // ------------------------------------------------------------------
    public void actualizarEstado(Long quejaId, String nuevoEstado,
                                  String comentario) throws SQLException {

        String estadoAnterior = obtenerEstado(quejaId);

        // Actualizar la queja
        String sqlUpdate = "UPDATE quejas SET estado = ?, "
                         + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
            ps.setString(1, nuevoEstado);
            ps.setLong(2, quejaId);
            ps.executeUpdate();
        }

        // Registrar en historial (trazabilidad)
        String sqlHistorial = "INSERT INTO historial_estados"
                            + "(queja_id, estado_anterior, estado_nuevo, comentario) "
                            + "VALUES(?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlHistorial)) {
            ps.setLong(1, quejaId);
            ps.setString(2, estadoAnterior);
            ps.setString(3, nuevoEstado);
            ps.setString(4, comentario);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Auxiliar: obtener estado actual
    // ------------------------------------------------------------------
    private String obtenerEstado(Long quejaId) throws SQLException {
        String sql = "SELECT estado FROM quejas WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, quejaId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("estado") : null;
        }
    }

    // ------------------------------------------------------------------
    // Auxiliar: mapear ResultSet -> Queja
    // ------------------------------------------------------------------
    private Queja mapearQueja(ResultSet rs) throws SQLException {
        Queja q = new Queja(
            rs.getLong("id"),
            rs.getLong("ciudadano_id"),
            rs.getString("descripcion"),
            rs.getString("empresa"),
            rs.getString("estado")
        );
        Timestamp fr = rs.getTimestamp("fecha_registro");
        Timestamp fa = rs.getTimestamp("fecha_actualizacion");
        if (fr != null) q.setFechaRegistro(fr.toLocalDateTime());
        if (fa != null) q.setFechaActualizacion(fa.toLocalDateTime());
        return q;
    }
}
