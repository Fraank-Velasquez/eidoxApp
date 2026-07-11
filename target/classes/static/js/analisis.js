
let datosAnalisisActuales = null;
const CLAVE_RESULTADO_ANALISIS = 'eidox-resultado-analisis';

function obtenerPanelAnalisis() {
    return document.querySelector('.analisis-panel');
}

function mostrarCargaAnalisis(mostrar) {
    const overlay = document.getElementById('analisisLoadingOverlay');
    const panel = obtenerPanelAnalisis();
    if (overlay) {
        overlay.hidden = !mostrar;
    }
    if (panel) {
        panel.classList.toggle('is-loading', mostrar);
    }
}

function contarPalabras(texto) {
    const limpio = texto.trim();
    return limpio === '' ? 0 : limpio.split(/\s+/).length;
}

document.addEventListener('DOMContentLoaded', () => {
    intentarRenderizarResultadoGuardado();
    const contenedorResultado = document.getElementById('resultado-analisis');
    if (contenedorResultado) {
        contenedorResultado.scrollIntoView({ behavior: 'auto', block: 'start' });
    }
});

function guardarResultadoAnalisis(datos) {
    try {
        sessionStorage.setItem(CLAVE_RESULTADO_ANALISIS, JSON.stringify(datos));
    } catch (error) {
        console.error('No se pudo guardar el resultado del analisis en sesion:', error);
    }
}

function intentarRenderizarResultadoGuardado() {
    const contenedorResultado = document.getElementById('resultado-analisis');
    if (!contenedorResultado) {
        return;
    }

    const resultadoGuardado = sessionStorage.getItem(CLAVE_RESULTADO_ANALISIS);
    if (!resultadoGuardado) {
        return;
    }

    try {
        const datos = JSON.parse(resultadoGuardado);
        if (datos && datos.exito) {
            renderizarResultado(datos);
        }
    } catch (error) {
        console.error('No se pudo recuperar el resultado del analisis guardado:', error);
    } finally {
        sessionStorage.removeItem(CLAVE_RESULTADO_ANALISIS);
    }
}

window.addEventListener('colores-fuente-actualizados', () => {
    if (datosAnalisisActuales) {
        renderizarResultado(datosAnalisisActuales);
    }
});

document.addEventListener('click', async (evento) => {
    const boton = evento.target.closest('#btnDescargarReporte');
    if (!boton) {
        return;
    }

    if (!datosAnalisisActuales) {
        mostrarToast('No hay un resultado de analisis disponible para generar el reporte.', boton);
        return;
    }

    const datosReporte = {
        porcentajePlagio: datosAnalisisActuales.porcentajePlagio,
        totalPalabras: datosAnalisisActuales.totalPalabras,
        textoOriginal: datosAnalisisActuales.textoOriginal,
        fuentes: datosAnalisisActuales.fuentes,
        nombreDocumento: datosAnalisisActuales.nombreDocumento || 'Documento analizado'
    };

    try {
        boton.disabled = true;
        const respuesta = await fetch('/api/analisis/reporte/descargar-pdf', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(datosReporte)
        });

        if (!respuesta.ok) {
            throw new Error('No se pudo generar el reporte');
        }

        const blob = await respuesta.blob();
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        const nombreArchivo = obtenerNombreArchivoDescarga(respuesta);
        enlace.href = url;
        enlace.download = nombreArchivo;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        URL.revokeObjectURL(url);
        mostrarToast('Reporte generado y descargado correctamente.', boton);
    } catch (error) {
        console.error('Error al descargar el reporte:', error);
        mostrarToast('Ocurrio un error al generar el reporte.', boton);
    } finally {
        boton.disabled = false;
    }
});

function obtenerNombreArchivoDescarga(respuesta) {
    const disposition = respuesta.headers.get('Content-Disposition');
    if (!disposition) {
        return 'reporte_originalidad.pdf';
    }

    const coincidencia = disposition.match(/filename="([^"]+)"/i);
    return coincidencia ? coincidencia[1] : 'reporte_originalidad.pdf';
}

/**
 * Punto de entrada principal.
 * Llamar con los datos reales del backend.
 * @param {Object} datos
 */
function renderizarResultado(datos) {
    datosAnalisisActuales = datos;
    actualizarTarjetasResumen(datos);
    construirBarraDesglose(datos.fuentes, datos.porcentajePlagio);
    construirLeyendaColores(datos.fuentes);
    construirTextoHighlighted(datos.textoOriginal, datos.fuentes);
    construirListaFuentes(datos.fuentes);
    actualizarContadorFuentes(datos.fuentes.length);
}

function actualizarTarjetasResumen(datos) {
    const porcentaje = datos.porcentajePlagio;
    const totalPalabras = datos.totalPalabras;

    animarContador('numeroPlagio', porcentaje);
    animarContador('numeroFuentes', datos.fuentes.length);
    animarContador('numeroPalabras', totalPalabras);
    animarContador('numeroOriginal', 100 - porcentaje);

    // Nivel de riesgo
    const elNivel = document.getElementById('nivelPlagio');
    if (elNivel) {
        const { texto, color } = calcularNivelRiesgo(porcentaje);
        elNivel.textContent = texto;
        elNivel.style.color = color;
    }

    // Medidor circular SVG
    animarMedidorCircular(porcentaje);
}

function calcularNivelRiesgo(porcentaje) {
    if (porcentaje <= 10) return { texto: 'Bajo riesgo', color: '#10b981' };
    if (porcentaje <= 25) return { texto: 'Riesgo medio', color: '#f59f0bda' };
    if (porcentaje <= 50) return { texto: 'Riesgo alto', color: '#ef4444' };
    return { texto: 'Crítico', color: '#991b1b' };
}

function animarMedidorCircular(porcentaje) {
    const circulo = document.getElementById('circuloPlagio');
    const numEl = document.getElementById('numeroPlagio');
    if (!circulo) return;

    const circunferencia = 314;
    const desplazamiento = circunferencia - (porcentaje / 100) * circunferencia;

    // Color según riesgo
    const color = porcentaje <= 10 ? '#10b981ee' : porcentaje <= 25 ? '#f59f0b8e' : '#ef4444';
    circulo.style.stroke = color;

    setTimeout(() => {
        circulo.style.strokeDashoffset = desplazamiento;
    }, 200);

    // Contador numérico animado
    animarContadorDirecto(numEl, porcentaje, 1400);
}


function construirBarraDesglose(fuentes, porcentajeTotal) {
    const contenedorBarra = document.getElementById('barraDesglose');
    const contenedorLeyenda = document.getElementById('leyendaDesglose');
    if (!contenedorBarra) return;

    contenedorBarra.innerHTML = '';
    contenedorLeyenda.innerHTML = '';

    // Segmento original
    const porcentajeOriginal = 100 - porcentajeTotal;
    agregarSegmento(contenedorBarra, porcentajeOriginal, '#e5e7eb', 'original');
    agregarItemLeyenda(contenedorLeyenda, 'Contenido original', '#898f99', porcentajeOriginal);

    // Segmentos por fuente
    fuentes.forEach((fuente, i) => {
        let colorSegmento = '#999999';
        if (typeof generadorColor !== 'undefined') {
            colorSegmento = generadorColor.obtenerColorVibrante(i);
        }
        agregarSegmento(contenedorBarra, fuente.porcentaje, colorSegmento, `fuente-${i}`);
        agregarItemLeyenda(contenedorLeyenda, fuente.nombre, colorSegmento, fuente.porcentaje);
    });
}

function agregarSegmento(contenedor, porcentaje, color, id) {
    const seg = document.createElement('div');
    seg.className = 'segmento-barra';
    seg.style.background = color;
    seg.style.width = '0%';
    seg.dataset.id = id;
    contenedor.appendChild(seg);
    requestAnimationFrame(() => {
        setTimeout(() => { seg.style.width = porcentaje + '%'; }, 100);
    });
}

function agregarItemLeyenda(contenedor, nombre, color, porcentaje) {
    const item = document.createElement('div');
    item.className = 'leyenda-item';
    item.innerHTML = `
        <span class="leyenda-punto" style="background:${color}"></span>
        <span>${recortarNombre(nombre, 22)} · <strong>${porcentaje}%</strong></span>
    `;
    contenedor.appendChild(item);
}

function construirLeyendaColores(fuentes) {
    const contenedor = document.getElementById('leyendaColores');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    fuentes.forEach((fuente, i) => {
        let color = { fondo: '#f0f0f0', texto: '#333333' };
        if (typeof generadorColor !== 'undefined') {
            color = generadorColor.obtenerColor(i);
        }

        const pastilla = document.createElement('span');
        pastilla.className = 'pastilla-leyenda';
        pastilla.textContent = recortarNombre(fuente.nombre, 18);
        pastilla.style.background = color.fondo;
        pastilla.style.color = color.texto;
        pastilla.dataset.indice = i;
        pastilla.title = fuente.nombre;

        pastilla.addEventListener('click', () => filtrarPorFuente(i));
        contenedor.appendChild(pastilla);
    });
}


function construirTextoHighlighted(textoOriginal, fuentes) {
    const contenedor = document.getElementById('textoAnalizado');
    if (!contenedor) return;

    const mapaReemplazos = {};
    fuentes.forEach((fuente, i) => {
        fuente.fragmentosEnTexto.forEach(frag => {
            mapaReemplazos[frag] = i;
        });
    });

    let textoHtml = textoOriginal;

    textoHtml = escaparHtml(textoHtml);

    Object.entries(mapaReemplazos).forEach(([fragmento, indice]) => {
        const fragmentoEsc = escaparHtml(fragmento);

        let marca;

        if (typeof generadorColor !== 'undefined') {
            const color = generadorColor.obtenerColor(indice);
            marca = `<span class="marca-plagio" data-fuente="${indice}" style="--fuente-bg: ${color.fondo}; --fuente-text: ${color.texto};" title="Coincidencia con: ${escaparAtributo(fuentes[indice].nombre)}">${fragmentoEsc}</span>`;
        } else {
            marca = `<span class="marca-plagio" data-fuente="${indice}" title="Coincidencia con: ${escaparAtributo(fuentes[indice].nombre)}">${fragmentoEsc}</span>`;
        }

        textoHtml = textoHtml.replace(fragmentoEsc, marca);
    });

    textoHtml = textoHtml.replace(/\n\n/g, '</p><p>').replace(/\n/g, '<br>');
    contenedor.innerHTML = `<p>${textoHtml}</p>`;

    contenedor.querySelectorAll('.marca-plagio').forEach(marca => {
        marca.addEventListener('click', () => {
            const indice = parseInt(marca.dataset.fuente);
            activarFuente(indice);
        });
    });
}


function construirListaFuentes(fuentes) {
    const lista = document.getElementById('listaFuentes');
    if (!lista) return;
    lista.innerHTML = '';

    fuentes.forEach((fuente, i) => {
        let color, colorBorde;
        if (typeof generadorColor !== 'undefined') {
            color = generadorColor.obtenerColor(i);
            colorBorde = generadorColor.obtenerColorBorde(i);
        } else {
            color = { fondo: '#f0f0f0', texto: '#333333' };
            colorBorde = '#999999';
        }

        const tarjeta = document.createElement('div');
        tarjeta.className = 'tarjeta-fuente';
        tarjeta.dataset.indice = i;
        tarjeta.style.setProperty('--fuente-border', colorBorde);
        tarjeta.style.borderLeftColor = colorBorde;
        tarjeta.style.animationDelay = `${i * 80}ms`;

        const iconoTipo = fuente.tipo === 'pdf' ? 'bi-file-earmark-pdf'
            : fuente.tipo === 'web' ? 'bi-globe'
                : fuente.tipo === 'word' ? 'bi-file-earmark-word'
                    : 'bi-file-earmark';

        tarjeta.innerHTML = `<div class="fuente-cabecera"><div class="fuente-nombre"><i class="bi ${iconoTipo}" style="color:${colorBorde}"></i><span title="${fuente.nombre}">${recortarNombre(fuente.nombre, 28)}</span></div><span class="fuente-porcentaje" style="background:${color.fondo}; color:${color.texto};">${fuente.porcentaje}%</span></div><div class="fuente-fragmento" style="background:${color.fondo}; border-color:${colorBorde}; color:${color.texto};">${fuente.fragmentoCoincidente}</div><div class="fuente-pie">${fuente.url ? `<a href="${fuente.url}" class="fuente-url" target="_blank" rel="noopener">${fuente.url}</a>` : `<span class="fuente-url">Documento interno</span>`}<span class="fuente-coincidencias">${fuente.fragmentosEnTexto.length} coincidencia${fuente.fragmentosEnTexto.length !== 1 ? 's' : ''}</span></div>`;

        tarjeta.addEventListener('click', () => activarFuente(i));
        lista.appendChild(tarjeta);
    });
}

function actualizarContadorFuentes(total) {
    const el = document.getElementById('fuentesContador');
    if (el) el.textContent = `${total} coincidencia${total !== 1 ? 's' : ''}`;
}

/*  Resaltar en texto y tarjeta al hacer clic */
let indiceFuenteActiva = null;

function activarFuente(indice) {
    // Si ya está activa, desactivar (toggle)
    if (indiceFuenteActiva === indice) {
        desactivarFuentes();
        return;
    }
    indiceFuenteActiva = indice;

    // Texto: atenuar todas excepto las del índice activo
    document.querySelectorAll('.marca-plagio').forEach(marca => {
        const estaFuente = parseInt(marca.dataset.fuente) === indice;
        marca.classList.toggle('activa', estaFuente);
        marca.classList.toggle('atenuada', !estaFuente);
    });

    // Tarjetas: resaltar la activa
    document.querySelectorAll('.tarjeta-fuente').forEach(tarjeta => {
        tarjeta.classList.toggle('activa', parseInt(tarjeta.dataset.indice) === indice);
    });

    // Scroll automático a la tarjeta activa
    const tarjetaActiva = document.querySelector(`.tarjeta-fuente[data-indice="${indice}"]`);
    tarjetaActiva?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

    // Scroll a la primera marca en el texto
    const primeraMarca = document.querySelector(`.marca-plagio[data-fuente="${indice}"]`);
    primeraMarca?.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function desactivarFuentes() {
    indiceFuenteActiva = null;
    document.querySelectorAll('.marca-plagio').forEach(m => {
        m.classList.remove('activa', 'atenuada');
    });
    document.querySelectorAll('.tarjeta-fuente').forEach(t => {
        t.classList.remove('activa');
    });
}

function filtrarPorFuente(indice) {
    activarFuente(indice);
}

/**
 * Calcula un color de borde oscuro basado en el color de fondo
 * Se usa para bordes de tarjetas y elementos visuales
 * @param {string} colorFondo - Color en formato hex
 * @returns {string} Color oscuro en formato hex
 */
function calcularColorBorde(colorFondo) {
    // Extraer valores RGB del color hex
    const r = parseInt(colorFondo.slice(1, 3), 16);
    const g = parseInt(colorFondo.slice(3, 5), 16);
    const b = parseInt(colorFondo.slice(5, 7), 16);

    // Calcular luminancia
    const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

    // Si es claro, oscurecer; si es oscuro, aclarar
    const factor = lum > 0.5 ? 0.6 : 1.4;

    const rNew = Math.round(r * factor).toString(16).padStart(2, '0');
    const gNew = Math.round(g * factor).toString(16).padStart(2, '0');
    const bNew = Math.round(b * factor).toString(16).padStart(2, '0');

    return `#${rNew}${gNew}${bNew}`;
}
function animarContador(idElemento, valorFinal, duracion = 1200) {
    const el = document.getElementById(idElemento);
    if (!el) return;
    const htmlExtra = el.innerHTML.replace(/^\d+/, '');
    animarContadorDirecto(el, valorFinal, duracion, htmlExtra);
}

function animarContadorDirecto(el, valorFinal, duracion = 1200, htmlExtra = '') {
    if (!el) return;
    const inicio = performance.now();

    const paso = (ahora) => {
        const transcurrido = ahora - inicio;
        const progreso = Math.min(transcurrido / duracion, 1);
        const easeOut = 1 - Math.pow(1 - progreso, 3);
        el.innerHTML = Math.round(easeOut * valorFinal) + htmlExtra;
        if (progreso < 1) requestAnimationFrame(paso);
    };
    requestAnimationFrame(paso);
}

function recortarNombre(nombre, maxChars) {
    if (nombre.length <= maxChars) return nombre;
    const ext = nombre.includes('.') ? '.' + nombre.split('.').pop() : '';
    const corte = maxChars - ext.length - 1;
    return nombre.substring(0, corte) + '…' + ext;
}

function escaparHtml(texto) {
    return texto
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function escaparAtributo(texto) {
    return texto.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}


//////////////////////////


const inputArchivo = document.getElementById('archivo');
const botonAnalizar = document.getElementById('btnAnalizar');

if (botonAnalizar && inputArchivo) {
    botonAnalizar.addEventListener('click', function (evento) {
        evento.preventDefault();
        const elementoToastA = document.getElementById('btnAnalizar');
        const elementoToastB = document.querySelector('.analisis-toolbar');
        const areaTexto = document.getElementById('txtAnalizar');
        const textoManual = areaTexto ? areaTexto.value : '';
        const palabrasTexto = contarPalabras(textoManual);
        const archivoSeleccionado = inputArchivo.files.length > 0 ? inputArchivo.files[0] : null;

        if (!archivoSeleccionado && palabrasTexto < 70) {
            mostrarToast("Escribe al menos 70 palabras o sube un documento para analizar.", elementoToastA)
            return;
        }

        const datosFormulario = new FormData();

        if (archivoSeleccionado) {
            datosFormulario.append("archivo", archivoSeleccionado);
        }

        if (!archivoSeleccionado && palabrasTexto >= 70) {
            datosFormulario.append("textoAnalisis", textoManual);
        }

        mostrarCargaAnalisis(true);
        botonAnalizar.disabled = true;
        botonAnalizar.classList.add('is-disabled');

        console.log("Enviando archivo al backend...");

        fetch('/api/analisis/subir', {
            method: 'POST',
            body: datosFormulario
        })
            .then(respuesta => respuesta.json())
            .then(datos => {
                mostrarCargaAnalisis(false);
                botonAnalizar.disabled = false;
                if (datos.exito) {

                    console.log("Respuesta del Servidor:", datos);
                    const nombreArchivo = archivoSeleccionado ? archivoSeleccionado.name : 'Texto manual';
                    datos.nombreDocumento = nombreArchivo;
                    guardarResultadoAnalisis(datos);
                    window.location.href = '/analisis';
                    mostrarToast("Análisis completado correctamente.", elementoToastB)

                } else {
                    mostrarToast(`Hubo un error: ${datos.mensaje}`, elementoToastB)
                }
            })
            .catch(error => {
                mostrarCargaAnalisis(false);
                botonAnalizar.disabled = false;
                console.error("Error en la conexión con el servidor:", error);
                mostrarToast("Error de red al intentar analizar el documento.", elementoToastB)
            });
    });
}
