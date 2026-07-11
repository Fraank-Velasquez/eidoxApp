package utp.eidox.estructura;

import java.util.ArrayList;
import java.util.List;

public class ArbolAVL {

    private NodoAVL raiz;

    public void insertar(String clave, ReferenciaDocumento referencia) {
        if (clave == null || clave.isBlank() || referencia == null) {
            return;
        }

        raiz = insertarRecursivo(raiz, clave, referencia);
    }

    public boolean existe(String clave) {
        return !buscar(clave).isEmpty();
    }

    public List<ReferenciaDocumento> buscar(String clave) {
        NodoAVL encontrado = buscarNodo(raiz, clave);
        if (encontrado == null) {
            return List.of();
        }

        return List.copyOf(encontrado.referencias);
    }

    private NodoAVL insertarRecursivo(NodoAVL nodoActual, String clave, ReferenciaDocumento referencia) {
        if (nodoActual == null) {
            return new NodoAVL(clave, referencia);
        }

        int comparacion = clave.compareTo(nodoActual.clave);

        if (comparacion < 0) {
            nodoActual.izquierdo = insertarRecursivo(nodoActual.izquierdo, clave, referencia);
        } else if (comparacion > 0) {
            nodoActual.derecho = insertarRecursivo(nodoActual.derecho, clave, referencia);
        } else {
            nodoActual.agregarReferencia(referencia);
            return nodoActual;
        }

        actualizarAltura(nodoActual);
        return balancear(nodoActual, clave);
    }

    private NodoAVL buscarNodo(NodoAVL nodoActual, String clave) {
        NodoAVL actual = nodoActual;

        while (actual != null) {
            int comparacion = clave.compareTo(actual.clave);

            if (comparacion == 0) {
                return actual;
            }

            actual = comparacion < 0 ? actual.izquierdo : actual.derecho;
        }

        return null;
    }

    private NodoAVL balancear(NodoAVL nodo, String claveInsertada) {
        int factorBalance = obtenerBalance(nodo);

        if (factorBalance > 1 && claveInsertada.compareTo(nodo.izquierdo.clave) < 0) {
            return rotacionDerecha(nodo);
        }

        if (factorBalance < -1 && claveInsertada.compareTo(nodo.derecho.clave) > 0) {
            return rotacionIzquierda(nodo);
        }

        if (factorBalance > 1 && claveInsertada.compareTo(nodo.izquierdo.clave) > 0) {
            nodo.izquierdo = rotacionIzquierda(nodo.izquierdo);
            return rotacionDerecha(nodo);
        }

        if (factorBalance < -1 && claveInsertada.compareTo(nodo.derecho.clave) < 0) {
            nodo.derecho = rotacionDerecha(nodo.derecho);
            return rotacionIzquierda(nodo);
        }

        return nodo;
    }

    private NodoAVL rotacionDerecha(NodoAVL nodoDesbalanceado) {
        NodoAVL nuevoPadre = nodoDesbalanceado.izquierdo;
        NodoAVL subarbolDerecho = nuevoPadre.derecho;

        nuevoPadre.derecho = nodoDesbalanceado;
        nodoDesbalanceado.izquierdo = subarbolDerecho;

        actualizarAltura(nodoDesbalanceado);
        actualizarAltura(nuevoPadre);

        return nuevoPadre;
    }

    private NodoAVL rotacionIzquierda(NodoAVL nodoDesbalanceado) {
        NodoAVL nuevoPadre = nodoDesbalanceado.derecho;
        NodoAVL subarbolIzquierdo = nuevoPadre.izquierdo;

        nuevoPadre.izquierdo = nodoDesbalanceado;
        nodoDesbalanceado.derecho = subarbolIzquierdo;

        actualizarAltura(nodoDesbalanceado);
        actualizarAltura(nuevoPadre);

        return nuevoPadre;
    }

    private void actualizarAltura(NodoAVL nodo) {
        nodo.altura = 1 + Math.max(obtenerAltura(nodo.izquierdo), obtenerAltura(nodo.derecho));
    }

    private int obtenerAltura(NodoAVL nodo) {
        return nodo == null ? 0 : nodo.altura;
    }

    private int obtenerBalance(NodoAVL nodo) {
        return nodo == null ? 0 : obtenerAltura(nodo.izquierdo) - obtenerAltura(nodo.derecho);
    }

    public List<ReferenciaDocumento> recorridoEnOrden() {
        List<ReferenciaDocumento> referencias = new ArrayList<>();
        recorridoEnOrden(raiz, referencias);
        return referencias;
    }

    private void recorridoEnOrden(NodoAVL nodo, List<ReferenciaDocumento> referencias) {
        if (nodo == null) {
            return;
        }

        recorridoEnOrden(nodo.izquierdo, referencias);
        referencias.addAll(nodo.referencias);
        recorridoEnOrden(nodo.derecho, referencias);
    }
}
