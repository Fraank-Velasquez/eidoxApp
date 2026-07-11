package utp.eidox.estructura;

import java.util.ArrayList;
import java.util.List;

class NodoAVL {

    String clave;
    List<ReferenciaDocumento> referencias;
    NodoAVL izquierdo;
    NodoAVL derecho;
    int altura;

    NodoAVL(String clave, ReferenciaDocumento referencia) {
        this.clave = clave;
        this.referencias = new ArrayList<>();
        this.referencias.add(referencia);
        this.altura = 1;
    }

    void agregarReferencia(ReferenciaDocumento referencia) {
        if (!this.referencias.contains(referencia)) {
            this.referencias.add(referencia);
        }
    }
}
