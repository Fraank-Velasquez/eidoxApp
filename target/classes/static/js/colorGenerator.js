/**
 * Generador de colores dinámicos para marcas de plagio
 * Genera colores HSL únicos y legibles para cada fuente
 */

class GeneradorColor {
    constructor() {
        this.coloresCache = new Map();
        this.observarModoOscuro();
    }

    /**
     * Observar cambios en el modo oscuro para actualizar colores en tiempo real
     */
    observarModoOscuro() {
        const observer = new MutationObserver(() => {
            this.coloresCache.clear();
            window.dispatchEvent(new CustomEvent('colores-fuente-actualizados'));
        });

        observer.observe(document.body, {
            attributes: true,
            attributeFilter: ['class']
        });
    }

    /**
     * Obtiene el color para una fuente específica
     * @param {number} indice - Índice de la fuente (0, 1, 2, ...)
     * @returns {Object} { fondo, texto } con valores hex
     */
    obtenerColor(indice) {
        if (this.coloresCache.has(indice)) {
            return this.coloresCache.get(indice);
        }

        const esModoOscuro = document.body.classList.contains('dark-mode');
        const color = this.generarColorHSL(indice, esModoOscuro);

        this.coloresCache.set(indice, color);
        return color;
    }

    /**
     * Genera color HSL basado en índice y modo
     * @param {number} indice - Índice de la fuente
     * @param {boolean} esModoOscuro - Si está en modo oscuro
     * @returns {Object} { fondo, texto } con valores hex
     */
    generarColorHSL(indice, esModoOscuro) {
        // Distribuye el matiz uniformemente en el espectro (0-360)
        const matiz = (indice * 60) % 360;

        let saturacion, luminosidadFondo, luminosidadTexto;

        if (esModoOscuro) {
            saturacion = 24;
            luminosidadFondo = 20;
            luminosidadTexto = 88;
        } else {
            saturacion = 48;
            luminosidadFondo = 90;
            luminosidadTexto = 32;
        }

        return {
            fondo: this.hslAHex(matiz, saturacion, luminosidadFondo),
            texto: this.hslAHex(matiz, saturacion, luminosidadTexto)
        };
    }


    hslAHex(h, s, l) {
        s /= 100;
        l /= 100;

        const k = n => (n + h / 30) % 12;
        const a = s * Math.min(l, 1 - l);
        const f = n => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));

        const r = Math.round(255 * f(0));
        const g = Math.round(255 * f(8));
        const b = Math.round(255 * f(4));

        return `#${[r, g, b].map(x => x.toString(16).padStart(2, '0')).join('')}`;
    }

    /**
     * Obtiene color más vibrante/saturado (para barras, iconos, etc)
     * @param {number} indice - Índice de la fuente
     * @returns {string} Color hex más vibrante
     */
    obtenerColorVibrante(indice) {
        const matiz = (indice * 60) % 360;
        const esModoOscuro = document.body.classList.contains('dark-mode');
        const saturacion = esModoOscuro ? 42 : 42;
        const luminosidad = esModoOscuro ? 62 : 66;
        return this.hslAHex(matiz, saturacion, luminosidad);
    }

    /**
     * Obtiene un color con más presencia para bordes e iconos.
     * @param {number} indice - Índice de la fuente
     * @returns {string} Color hex para acentos
     */
    obtenerColorBorde(indice) {
        const matiz = (indice * 60) % 360;
        const esModoOscuro = document.body.classList.contains('dark-mode');
        const saturacion = esModoOscuro ? 52 : 64;
        const luminosidad = esModoOscuro ? 66 : 42;
        return this.hslAHex(matiz, saturacion, luminosidad);
    }

    /**
     * Aplica colores a múltiples elementos
     * @param {Array} elementos - Array de elementos con propiedades { elemento, indice }
     */
    aplicarColoresEnLote(elementos) {
        elementos.forEach(({ elemento, indice }) => {
            this.aplicarColorAlElemento(elemento, indice);
        });
    }
}

const generadorColor = new GeneradorColor();
