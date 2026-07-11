package utp.eidox.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import utp.eidox.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    boolean existsByCorreo(String correo);

}
