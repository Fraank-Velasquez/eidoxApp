package utp.eidox.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import utp.eidox.model.Analisis;

public interface AnalisisRepository extends JpaRepository<Analisis, Long> {

    List<Analisis> findByUsuarioIdUsuarioOrderByFechaAnalisisDesc(Long idUsaurio);
}
