package utp.eidox.util;

import java.util.ArrayList;
import java.util.List;

public class OrdenadorQuicksort {

    public List<String> ordenar(List<String> elementos) {
        if (elementos == null || elementos.size() <= 1) {
            return elementos == null ? List.of() : List.copyOf(elementos);
        }

        List<String> copia = new ArrayList<>(elementos);
        quicksort(copia, 0, copia.size() - 1);
        return copia;
    }

    private void quicksort(List<String> elementos, int inicio, int fin) {
        if (inicio >= fin) {
            return;
        }

        int indicePivote = particionar(elementos, inicio, fin);
        quicksort(elementos, inicio, indicePivote - 1);
        quicksort(elementos, indicePivote + 1, fin);
    }

    private int particionar(List<String> elementos, int inicio, int fin) {
        String pivote = elementos.get(fin);
        int indiceMenor = inicio;

        for (int indiceActual = inicio; indiceActual < fin; indiceActual++) {
            if (elementos.get(indiceActual).compareTo(pivote) <= 0) {
                intercambiar(elementos, indiceMenor, indiceActual);
                indiceMenor++;
            }
        }

        intercambiar(elementos, indiceMenor, fin);
        return indiceMenor;
    }

    private void intercambiar(List<String> elementos, int primero, int segundo) {
        String temporal = elementos.get(primero);
        elementos.set(primero, elementos.get(segundo));
        elementos.set(segundo, temporal);
    }
}
