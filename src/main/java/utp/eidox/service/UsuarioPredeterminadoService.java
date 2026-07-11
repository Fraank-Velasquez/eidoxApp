package utp.eidox.service;

import org.springframework.stereotype.Service;

import utp.eidox.model.Usuario;
import utp.eidox.repository.UsuarioRepository;

@Service
public class UsuarioPredeterminadoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioPredeterminadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtenerOCrearUsuarioSistema() {
        return usuarioRepository.findByNombreUsuario("demo_utp")
                .orElseGet(() -> usuarioRepository.save(crearUsuarioBase()));
    }

    private Usuario crearUsuarioBase() {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("demo_utp");
        usuario.setPassword("demo1234");
        usuario.setCorreo("demo@utp.edu.pe");
        return usuario;
    }
}
