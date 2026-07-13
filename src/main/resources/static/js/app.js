if (localStorage.getItem('theme') === 'dark') {
    document.body.classList.add('dark-mode');
}

document.addEventListener('DOMContentLoaded', () => {

    sincronizarEtiquetaTemaMovil();
    iniciarScrollNavegacion();
    iniciarNavegacionMovil();
    iniciarParticulas();
    iniciarParallax();
    iniciarReveladoScroll();
    iniciarContadores();
    iniciarAreaTexto();
    iniciarSubidaArchivo();

});

function iniciarScrollNavegacion() {
    const barraNavegacion = document.getElementById('navbar');
    const alHacerScroll = () => {
        barraNavegacion.classList.toggle('scrolled', window.scrollY > 30);
    };
    window.addEventListener('scroll', alHacerScroll, { passive: true });
    alHacerScroll();
}

function iniciarNavegacionMovil() {
    const botonAlternar = document.getElementById('navToggle');
    const fondoOscuro = document.getElementById('navBackdrop');
    const panel = document.getElementById('mobileMenu');
    const botonCerrar = document.getElementById('navClose');

    if (!botonAlternar || !fondoOscuro || !panel) return;

    const abrirMenu = () => {
        panel.classList.add('is-open');
        fondoOscuro.hidden = false;
        botonAlternar.setAttribute('aria-expanded', 'true');
        panel.setAttribute('aria-hidden', 'false');
        document.body.classList.add('nav-menu-open');
    };

    const cerrarMenu = () => {
        panel.classList.remove('is-open');
        fondoOscuro.hidden = true;
        botonAlternar.setAttribute('aria-expanded', 'false');
        panel.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('nav-menu-open');
    };

    const alternarMenu = () => {
        if (panel.classList.contains('is-open')) {
            cerrarMenu();
            return;
        }

        abrirMenu();
    };

    botonAlternar.addEventListener('click', alternarMenu);
    fondoOscuro.addEventListener('click', cerrarMenu);
    botonCerrar?.addEventListener('click', cerrarMenu);

    panel.querySelectorAll('a').forEach(enlace => {
        enlace.addEventListener('click', cerrarMenu);
    });

    window.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape') {
            cerrarMenu();
        }
    });

    window.addEventListener('resize', () => {
        if (window.innerWidth > 700) {
            cerrarMenu();
        }
    }, { passive: true });
}

function sincronizarEtiquetaTemaMovil() {
    const etiqueta = document.getElementById('mobileThemeLabel');
    const boton = document.getElementById('mobileThemeToggle');
    if (!etiqueta || !boton) return;

    const esOscuro = document.body.classList.contains('dark-mode');
    const siguienteEtiqueta = esOscuro ? 'Modo claro' : 'Modo oscuro';
    etiqueta.textContent = siguienteEtiqueta;
    boton.setAttribute('aria-label', siguienteEtiqueta);
}

function iniciarParticulas() {
    const canvas = document.getElementById('particles-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    const PALABRAS = ['original', 'Eidox', 'plagio', 'verificado', 'auténtico', 'análisis', 'preciso', 'fuente', 'cita', 'texto'];
    const particulas = [];
    let ancho, alto;

    function redimensionar() {
        ancho = canvas.width = window.innerWidth;
        alto = canvas.height = window.innerHeight;
    }
    redimensionar();
    window.addEventListener('resize', redimensionar, { passive: true });

    const esOscuro = () => document.body.classList.contains('dark-mode');

    class Particula {
        constructor() {
            this.reiniciar();
        }
        reiniciar() {
            this.palabra = PALABRAS[Math.floor(Math.random() * PALABRAS.length)];
            this.x = Math.random() * ancho;
            this.y = alto + 30;
            this.velocidad = 0.25 + Math.random() * 0.45;
            this.dx = (Math.random() - .5) * 0.3;
            this.tamano = 10 + Math.random() * 8;
            this.alfa = 0;
            this.alfaObjetivo = 0.06 + Math.random() * 0.09;
            this.rotacion = (Math.random() - .5) * 0.4;
        }
        actualizar() {
            this.y -= this.velocidad;
            this.x += this.dx;
            if (this.y < alto * 0.6 && this.alfa < this.alfaObjetivo) this.alfa += 0.002;
            if (this.y < alto * 0.15) this.alfa -= 0.003;
            if (this.alfa <= 0 && this.y < 0) this.reiniciar();
        }
        dibujar() {
            ctx.save();
            ctx.translate(this.x, this.y);
            ctx.rotate(this.rotacion);
            ctx.globalAlpha = Math.max(0, this.alfa);
            ctx.font = `${this.tamano}px 'Syne', sans-serif`;
            ctx.fillStyle = esOscuro() ? '#ffffff' : '#1c1c1d';
            ctx.fillText(this.palabra, 0, 0);
            ctx.restore();
        }
    }

    for (let i = 0; i < 28; i++) {
        const p = new Particula();
        p.y = Math.random() * alto;
        p.alfa = p.alfaObjetivo * Math.random();
        particulas.push(p);
    }

    function bucle() {
        ctx.clearRect(0, 0, ancho, alto);
        particulas.forEach(p => { p.actualizar(); p.dibujar(); });
        requestAnimationFrame(bucle);
    }
    bucle();
}

function iniciarParallax() {
    const orbes = document.querySelectorAll('[data-parallax]');
    if (!orbes.length) return;

    const alHacerScroll = () => {
        const scrollY = window.scrollY;
        orbes.forEach(orbe => {
            const velocidad = parseFloat(orbe.dataset.parallax);
            orbe.style.transform = `translateY(${scrollY * velocidad}px)`;
        });
    };
    window.addEventListener('scroll', alHacerScroll, { passive: true });
}

function iniciarReveladoScroll() {
    const secciones = document.querySelectorAll('.fade-section');
    if (!secciones.length) return;

    const observador = new IntersectionObserver((entradas) => {
        entradas.forEach(entrada => {
            if (entrada.isIntersecting) {
                entrada.target.classList.add('visible');
            }
        });
    }, { threshold: 0.12 });

    secciones.forEach(s => observador.observe(s));
}


function iniciarContadores() {
    const contadores = document.querySelectorAll('.counter-anim, [data-target]');
    if (!contadores.length) return;

    const observador = new IntersectionObserver((entradas) => {
        entradas.forEach(entrada => {
            if (entrada.isIntersecting) {
                animarContadorEstadisticas(entrada.target);
                observador.unobserve(entrada.target);
            }
        });
    }, { threshold: 0.5 });

    contadores.forEach(c => observador.observe(c));
}

function animarContadorEstadisticas(elemento) {
    const objetivo = parseInt(elemento.dataset.target, 10);
    const duracion = 1400;
    const inicio = performance.now();

    const paso = (ahora) => {
        const transcurrido = ahora - inicio;
        const progreso = Math.min(transcurrido / duracion, 1);
        const suavizado = 1 - Math.pow(1 - progreso, 3); // ease-out-cubic
        elemento.textContent = Math.round(suavizado * objetivo);
        if (progreso < 1) requestAnimationFrame(paso);
    };
    requestAnimationFrame(paso);
}


function iniciarAreaTexto() {
    const areaTexto = document.getElementById('txtAnalizar');
    const contador = document.getElementById('contadortxt');
    const btnPapelera = document.getElementById('btnTrash');
    const barraProgreso = document.getElementById('wordProgress');
    const botonAnalizar = document.getElementById('btnAnalizar');
    const notaAnalisis = document.getElementById('analysisNote');
    if (!areaTexto) return;

    const MAX_PALABRAS = 1000;
    const MIN_PALABRAS_ANALISIS = 70;

    const actualizarEstado = () => {
        const texto = areaTexto.value.trim();
        const palabras = texto === '' ? 0 : texto.split(/\s+/).length;

        contador.textContent = palabras;

        const porcentaje = Math.min((palabras / MAX_PALABRAS) * 100, 100);
        barraProgreso.style.width = porcentaje + '%';
        barraProgreso.classList.toggle('danger', palabras > MAX_PALABRAS * 0.85);

        btnPapelera.style.display = texto === '' ? 'none' : 'flex';

        if (botonAnalizar) {
            const hayTextoSuficiente = palabras >= MIN_PALABRAS_ANALISIS;
            botonAnalizar.classList.toggle('is-disabled', !hayTextoSuficiente && !document.getElementById('archivo')?.files[0]);
            botonAnalizar.disabled = !hayTextoSuficiente && !document.getElementById('archivo')?.files[0];
        }

        if (notaAnalisis) {
            notaAnalisis.textContent = palabras >= MIN_PALABRAS_ANALISIS
                ? 'Texto listo para analizar.'
                : `Escribe al menos ${MIN_PALABRAS_ANALISIS} palabras o sube un documento para habilitar el análisis.`;
        }
    };

    areaTexto.addEventListener('input', actualizarEstado);
    areaTexto.addEventListener('click', () => {
        const entradaArchivo = document.getElementById('archivo');
        const elementoToast = document.querySelector('.textarea-wrap');

        if (entradaArchivo.files[0]) {
            const archivo = entradaArchivo.files[0];
            const extension = archivo.name.slice(archivo.name.lastIndexOf('.')).toLowerCase();

            mostrarToast(`Ya tienes un archivo ${extension} cargado. Quítalo para ingresar texto manualmente.`, elementoToast);
            areaTexto.blur();
        }
    });
}

function mostrarToast(mensaje, elementoReferencia) {
    document.querySelector('.toast-archivo')?.remove();

    const toast = document.createElement('div');
    toast.className = 'toast-archivo';
    toast.innerHTML = `<i class="bi bi-info-circle"></i> ${mensaje}`;
    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add('toast-salir'), 2600);
    setTimeout(() => toast.remove(), 3800);
}

function borrarTexto() {
    const areaTexto = document.getElementById('txtAnalizar');
    const contador = document.getElementById('contadortxt');
    const btnPapelera = document.getElementById('btnTrash');
    const barraProgreso = document.getElementById('wordProgress');
    if (!areaTexto) return;

    areaTexto.value = '';
    contador.textContent = '0';
    barraProgreso.style.width = '0%';
    barraProgreso.classList.remove('danger');
    btnPapelera.style.display = 'none';
    areaTexto.focus();
}

function iniciarSubidaArchivo() {
    const entradaArchivo = document.getElementById('archivo');
    const btnSubir = document.getElementById('btn-upload')
    const etiquetaNombre = document.querySelector('.archivo-nombre');
    const botonQuitar = document.querySelector('.archivo-remove')
    const areaTexto = document.getElementById('txtAnalizar');
    const botonAnalizar = document.getElementById('btnAnalizar');
    if (!entradaArchivo) return;

    btnSubir.addEventListener('click', (evento) => {
        if (entradaArchivo.files[0]) {
            evento.preventDefault();
            mostrarToast('Solo se puede subir un archivo por análisis.', btnSubir);
            return;
        };

    });

    entradaArchivo.addEventListener('change', () => {

        const archivo = entradaArchivo.files[0];
        if (!archivo) return;


        const tiposValidos = [
            'application/pdf',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/msword'
        ];

        if (!tiposValidos.includes(archivo.type)) {
            entradaArchivo.value = '';
            mostrarToast("Solo se permiten cargar documentos PDF o Word", btnSubir)
            return;
        }

        const TAMANO_MAXIMO_BYTES = 10 * 1024 * 1024;
        if (archivo.size > TAMANO_MAXIMO_BYTES) {
            entradaArchivo.value = '';
            mostrarToast("El archivo supera el tamaño máximo permitido de 10 MB", btnSubir);
            return;
        }

        const nombre = entradaArchivo.files[0]?.name || '';
        if (etiquetaNombre) etiquetaNombre.textContent = nombre ? `📄 ${nombre} ` : '';

        if (botonQuitar) botonQuitar.classList.toggle('d-none', !nombre);

        if (nombre) {
            areaTexto.setAttribute('readOnly', true);
            areaTexto.setAttribute('placeholder', '');
            borrarTexto();
            botonAnalizar?.classList.remove('is-disabled');
            botonAnalizar && (botonAnalizar.disabled = false);
        }

    });

    botonQuitar.addEventListener('click', () => {

        entradaArchivo.value = '';

        if (etiquetaNombre) etiquetaNombre.textContent = '';
        botonQuitar.classList.add('d-none')
        areaTexto.removeAttribute('readOnly');
        areaTexto.setAttribute('placeholder', 'Ingresa tu texto o carga un documento para el análisis...');
        if (botonAnalizar) {
            const palabras = areaTexto.value.trim() === '' ? 0 : areaTexto.value.trim().split(/\s+/).length;
            const habilitado = palabras >= 70;
            botonAnalizar.classList.toggle('is-disabled', !habilitado);
            botonAnalizar.disabled = !habilitado;
        }


    });

}
function modoOscuro() {
    document.body.classList.toggle('dark-mode');
    const esOscuro = document.body.classList.contains('dark-mode');
    localStorage.setItem('theme', esOscuro ? 'dark' : 'light');
    sincronizarEtiquetaTemaMovil();
}