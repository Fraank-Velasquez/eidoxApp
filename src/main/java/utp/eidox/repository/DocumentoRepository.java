package utp.eidox.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import utp.eidox.model.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
}
