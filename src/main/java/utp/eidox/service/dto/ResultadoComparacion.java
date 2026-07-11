package utp.eidox.service.dto;

import java.util.List;

public record ResultadoComparacion(double porcentajePlagio, int totalPalabras, String textoOriginal,
        List<FuenteCoincidencia> fuentes) {
}
