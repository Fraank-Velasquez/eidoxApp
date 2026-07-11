package utp.eidox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4 , max = 10, message = "4-10 caracteres")
    @Column(nullable = false, length = 10,unique = true)
    private String nombreUsuario;

    @NotBlank(message = "La contraseña no puede estar vacia")
    @Column(nullable = false)
    private String password ;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ser un formato de correo obligatorio")
    @Column(nullable = false)
    private String correo;

}
