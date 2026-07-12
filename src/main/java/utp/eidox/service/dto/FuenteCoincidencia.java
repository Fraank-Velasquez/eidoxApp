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
public class FuenteCoincidencia {
    private String nombre;
    private String tipo;
    private String url;
    private double porcentaje;
    private String fragmentoCoincidente;
    private List<String> fragmentosEnTexto;
}
