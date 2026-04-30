package mx.gob.profeco.quejas.model;

/**
 * DTO para actualizar el estado de una queja.
 * Usado en PUT /api/quejas/{id}/estado
 */
public class EstadoDTO {

    private String estado;      // EN_PROCESO | RESUELTA | CERRADA
    private String comentario;  // Comentario opcional del funcionario

    public EstadoDTO() {}

    public String getEstado()      { return estado; }
    public void setEstado(String v) { this.estado = v; }

    public String getComentario()      { return comentario; }
    public void setComentario(String v) { this.comentario = v; }
}
