package utp.eidox.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import utp.eidox.model.Documento;

import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    Optional<Documento> findByNombreArchivo(String nombreArchivo);
}
