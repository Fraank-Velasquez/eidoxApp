package utp.eidox.service.dto;

import java.util.List;

public record FuenteCoincidencia(String nombre, String tipo, String url, double porcentaje,
        String fragmentoCoincidente, List<String> fragmentosEnTexto) {
}
