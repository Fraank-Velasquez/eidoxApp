package utp.eidox.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "analisis")
public class Analisis {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long idAnalisis;

@Column(name = "porcentaje_plagio", nullable = false)
private Double porcentaje;

@Lob
@Column(name = "referencias_encontradas", columnDefinition = "LONGTEXT")
private String referenciasEncontradas;

@Column(name = "fecha_analisis", updatable = false)
private LocalDate fechaAnalisis;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_usuario",nullable = false)
private Usuario usuario;

@PrePersist
protected void agregarFecha(){
    this.fechaAnalisis = LocalDate.now();
}

}
