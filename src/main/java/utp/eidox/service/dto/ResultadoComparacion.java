package utp.eidox.service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResultadoComparacion {
    private double porcentajePlagio;
    private int totalPalabras;
    private String textoOriginal;
    private List<FuenteCoincidencia> fuentes;
}
