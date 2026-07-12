class GeneradorColor {
    constructor() {
        this.coloresCache = new Map();
        this.observarModoOscuro();
    }

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

    obtenerColor(indice) {
        if (this.coloresCache.has(indice)) {
            return this.coloresCache.get(indice);
        }

        const esModoOscuro = document.body.classList.contains('dark-mode');
        const color = this.generarColorHSL(indice, esModoOscuro);

        this.coloresCache.set(indice, color);
        return color;
    }

    generarColorHSL(indice, esModoOscuro) {

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

    obtenerColorVibrante(indice) {
        const matiz = (indice * 60) % 360;
        const esModoOscuro = document.body.classList.contains('dark-mode');
        const saturacion = esModoOscuro ? 42 : 42;
        const luminosidad = esModoOscuro ? 62 : 66;
        return this.hslAHex(matiz, saturacion, luminosidad);
    }

    obtenerColorBorde(indice) {
        const matiz = (indice * 60) % 360;
        const esModoOscuro = document.body.classList.contains('dark-mode');
        const saturacion = esModoOscuro ? 52 : 64;
        const luminosidad = esModoOscuro ? 66 : 42;
        return this.hslAHex(matiz, saturacion, luminosidad);
    }

    aplicarColoresEnLote(elementos) {
        elementos.forEach(({ elemento, indice }) => {
            this.aplicarColorAlElemento(elemento, indice);
        });
    }
}

const generadorColor = new GeneradorColor();
