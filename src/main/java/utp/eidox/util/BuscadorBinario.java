package utp.eidox.util;

import java.util.List;

public class BuscadorBinario {

    public int buscarIndice(List<String> elementosOrdenados, String objetivo) {
        if (elementosOrdenados == null || elementosOrdenados.isEmpty() || objetivo == null) {
            return -1;
        }

        int inicio = 0;
        int fin = elementosOrdenados.size() - 1;

        while (inicio <= fin) {
            int medio = inicio + (fin - inicio) / 2;
            int comparacion = elementosOrdenados.get(medio).compareTo(objetivo);

            if (comparacion == 0) {
                return medio;
            }

            if (comparacion < 0) {
                inicio = medio + 1;
            } else {
                fin = medio - 1;
            }
        }

        return -1;
    }
}
