package mx.gob.profeco.quejas.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa los metadatos de un archivo de evidencia.
 * El archivo binario se almacena en el repositorio de archivos (disco).
 * Solo los metadatos se persisten en la BD SQL mediante JDBC.
 */
public class Evidencia {

    private Long   id;
    private Long   quejaId;
    private String nombreArchivo;
    private String rutaArchivo;   // ruta absoluta en el repositorio local
    private String tipoMime;
    private LocalDateTime fechaSubida;

    public Evidencia() {}

    public Evidencia(Long quejaId, String nombreArchivo,
                     String rutaArchivo, String tipoMime) {
        this.quejaId       = quejaId;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo   = rutaArchivo;
        this.tipoMime      = tipoMime;
        this.fechaSubida   = LocalDateTime.now();
    }

    public Long getId()             { return id; }
    public void setId(Long id)       { this.id = id; }

    public Long getQuejaId()         { return quejaId; }
    public void setQuejaId(Long v)   { this.quejaId = v; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String v) { this.nombreArchivo = v; }

    public String getRutaArchivo()   { return rutaArchivo; }
    public void setRutaArchivo(String v) { this.rutaArchivo = v; }

    public String getTipoMime()      { return tipoMime; }
    public void setTipoMime(String v) { this.tipoMime = v; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime v) { this.fechaSubida = v; }
}
