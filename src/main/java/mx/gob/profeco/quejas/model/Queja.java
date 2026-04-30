package mx.gob.profeco.quejas.model;

import java.time.LocalDateTime;

/**
 * Entidad Queja - representa una queja registrada en la BD.
 * Se serializa/deserializa a JSON usando Jackson (quarkus-rest-jackson),
 * tal como se vio en las diapositivas de Serialización.
 */
public class Queja {

    private Long id;
    private Long ciudadanoId;
    private String descripcion;
    private String empresa;
    private String estado;          // REGISTRADA | EN_PROCESO | RESUELTA | CERRADA
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    // Constructor vacío requerido por Jackson para deserialización
    public Queja() {}

    public Queja(Long id, Long ciudadanoId, String descripcion,
                 String empresa, String estado) {
        this.id           = id;
        this.ciudadanoId  = ciudadanoId;
        this.descripcion  = descripcion;
        this.empresa      = empresa;
        this.estado       = estado;
        this.fechaRegistro = LocalDateTime.now();
    }

    // ---------- Getters y Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCiudadanoId() { return ciudadanoId; }
    public void setCiudadanoId(Long ciudadanoId) { this.ciudadanoId = ciudadanoId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime f) { this.fechaRegistro = f; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime f) { this.fechaActualizacion = f; }
}
