package mx.gob.profeco.quejas.model;

/**
 * DTO (Data Transfer Object) para recibir el JSON del cliente
 * en el endpoint POST /api/quejas.
 *
 * Separa la representación de red del modelo interno,
 * patrón visto en las diapositivas de REST y Serialización.
 */
public class QuejaDTO {

    private Long   ciudadanoId;
    private String descripcion;
    private String empresa;

    // Constructor vacío para Jackson
    public QuejaDTO() {}

    public Long getCiudadanoId()    { return ciudadanoId; }
    public void setCiudadanoId(Long v) { this.ciudadanoId = v; }

    public String getDescripcion()     { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }

    public String getEmpresa()       { return empresa; }
    public void setEmpresa(String v) { this.empresa = v; }
}
