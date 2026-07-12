package utp.eidox.estructura;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReferenciaDocumento {
    private Long idDocumento;
    private String nombreDocumento;
    private String tipoDocumento;
    private String fragmentoCoincidente;
}

