# Implementación de Colores Dinámicos para Fuentes Plagiadas

## ✅ Qué se cambió

1. **CSS**: Removidas las clases `.marca-fuente-1` a `.marca-fuente-6`
2. **CSS**: `.marca-plagio` ahora usa variables dinámicas `--fuente-bg` y `--fuente-text`
3. **JavaScript**: Nuevo archivo `colorGenerator.js` que genera colores HSL automáticamente

---

## 📝 Cómo implementarlo en tu HTML

### Paso 1: Incluir el script en tu HTML (analisis.html o la página donde muestres resultados)

```html
<script src="/js/colorGenerator.js"></script>
```

**Agregalo ANTES del script que renderiza los análisis.**

---

### Paso 2: Aplicar colores al renderizar texto plagiado

Si estás generando HTML desde Thymeleaf o JavaScript, aplica el color así:

#### Opción A: Desde Thymeleaf (recomendado)

```html
<th:block th:each="match,stat : ${plagios}">
    <span class="marca-plagio" 
          th:data-indice="${stat.index}"
          th:text="${match.texto}">texto</span>
</th:block>

<script>
document.querySelectorAll('.marca-plagio').forEach(elem => {
    const indice = elem.getAttribute('data-indice');
    colorGen.aplicarColorAlElemento(elem, indice);
});
</script>
```

#### Opción B: Desde JavaScript

```javascript
// Cuando creas un elemento de plagio
const elemento = document.createElement('span');
elemento.classList.add('marca-plagio');
elemento.textContent = 'texto plagiado';

// Aplica el color (indice = 0, 1, 2, ...)
colorGen.aplicarColorAlElemento(elemento, indiceFuente);
```

#### Opción C: En lote (si tienes muchos elementos)

```javascript
const elementosConIndices = [
    { elemento: document.querySelector('.plagio-1'), indice: 0 },
    { elemento: document.querySelector('.plagio-2'), indice: 1 },
    { elemento: document.querySelector('.plagio-3'), indice: 2 }
];

colorGen.aplicarColoresEnLote(elementosConIndices);
```

---

## 🎨 Características

✅ **Escalable**: Soporta infinitas fuentes (1, 10, 50, 100...)  
✅ **Adaptable**: Se ajusta automáticamente a modo claro y oscuro  
✅ **Dinámico**: Los colores cambian instantáneamente al cambiar de modo  
✅ **Cacheado**: Memoriza colores para mejor rendimiento  
✅ **Sin hardcoding**: No necesitas definir clases CSS para cada fuente

---

## 🎯 Ejemplo completo

```html
<!-- En tu página de análisis -->
<div class="texto-analizado" id="textoAnalizado">
    <!-- Los spans de plagio serán generados aquí -->
</div>

<script src="/js/colorGenerator.js"></script>
<script>
    function renderizarTextoConPlagio(datos) {
        const container = document.getElementById('textoAnalizado');
        let html = '';
        
        datos.palabras.forEach((palabra, indiceGlobal) => {
            if (palabra.esplagio) {
                // aplica clase y color dinámico
                html += `<span class="marca-plagio" data-indice="${palabra.indiceFuente}">${palabra.texto}</span>`;
            } else {
                html += palabra.texto;
            }
        });
        
        container.innerHTML = html;
        
        // Aplicar colores a todos los elementos
        document.querySelectorAll('.marca-plagio').forEach(elem => {
            const indice = elem.getAttribute('data-indice');
            colorGen.aplicarColorAlElemento(elem, indice);
        });
    }
    
    // Llamar cuando tengas los datos
    renderizarTextoConPlagio(misDatos);
</script>
```

---

## 🌓 Cómo funciona en Dark Mode

**Modo Claro:**
- Fondo: Color claro (85% lightness)
- Texto: Color oscuro (25% lightness)

**Modo Oscuro:**
- Fondo: Color oscuro (25% lightness)
- Texto: Color claro (85% lightness)

El cambio es automático cuando agregas/quitas la clase `dark-mode` a `<body>`.

---

## 📌 Notas importantes

- El archivo `colorGenerator.js` debe cargarse **antes** de usarlo
- Usa `colorGen.obtenerColor(indice)` para obtener el objeto con `fondo` y `texto`
- Los colores se cachean por rendimiento; se limpian automáticamente al cambiar modo
- Compatible con todos los navegadores modernos (Chrome, Firefox, Safari, Edge)
