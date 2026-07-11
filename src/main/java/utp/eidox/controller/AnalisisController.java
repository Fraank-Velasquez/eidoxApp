package utp.eidox.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import utp.eidox.model.Documento;
import utp.eidox.repository.AnalisisRepository;
import utp.eidox.repository.DocumentoRepository;
import utp.eidox.service.ComparadorSimilitudService;
import utp.eidox.service.ExtraccionTikaService;
import utp.eidox.service.UsuarioPredeterminadoService;
import utp.eidox.service.dto.FuenteCoincidencia;
import utp.eidox.service.dto.ResultadoComparacion;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@RestController
@RequestMapping("/api/analisis")
public class AnalisisController {

    private static final ObjectMapper SERIALIZADOR_JSON = new ObjectMapper();

    private final ExtraccionTikaService extraccionService;
    private final ComparadorSimilitudService comparadorSimilitudService;
    private final DocumentoRepository documentoRepository;
    private final AnalisisRepository analisisRepository;
    private final UsuarioPredeterminadoService usuarioPredeterminadoService;

    public AnalisisController(ExtraccionTikaService extraccionService,
            ComparadorSimilitudService comparadorSimilitudService,
            DocumentoRepository documentoRepository, AnalisisRepository analisisRepository,
            UsuarioPredeterminadoService usuarioPredeterminadoService) {
        this.extraccionService = extraccionService;
        this.comparadorSimilitudService = comparadorSimilitudService;
        this.documentoRepository = documentoRepository;
        this.analisisRepository = analisisRepository;
        this.usuarioPredeterminadoService = usuarioPredeterminadoService;
    }

    @PostMapping("/subir")
    public ResponseEntity<Map<String, Object>> analizarDocumento(@RequestParam("archivo") MultipartFile archivo) {
        return analizarDocumentoInterno(archivo, null);
    }

    @PostMapping(value = "/subir", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, Object>> analizarTexto(
            @RequestParam(value = "textoAnalisis", required = false) String textoAnalisis) {

        return analizarDocumentoInterno(null, textoAnalisis);
    }

    @PostMapping(value = "/subir", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> analizarMixto(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "textoAnalisis", required = false) String textoAnalisis) {

        return analizarDocumentoInterno(archivo, textoAnalisis);
    }

    @PostMapping(value = "/reporte/descargar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> descargarReporte(@RequestBody Map<String, Object> resultadoAnalisis) {
        try {
            ResultadoComparacion resultado = convertirResultado(resultadoAnalisis);

            String nombreDocumento = resultadoAnalisis.get("nombreDocumento") != null
                    ? String.valueOf(resultadoAnalisis.get("nombreDocumento"))
                    : "Documento analizado";

            String contenidoReporte = construirReporteTexto(resultado, nombreDocumento);
            String marcaTiempo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nombreArchivo = "reporte_originalidad_" + marcaTiempo + ".txt";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(contenidoReporte.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/reporte/descargar-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> descargarReportePdf(@RequestBody Map<String, Object> resultadoAnalisis) {
        try {
            ResultadoComparacion resultado = convertirResultado(resultadoAnalisis);

            String nombreDocumento = resultadoAnalisis.get("nombreDocumento") != null
                    ? String.valueOf(resultadoAnalisis.get("nombreDocumento"))
                    : "Documento analizado";

            byte[] contenidoPdf = construirReportePdf(resultado, nombreDocumento);
            String marcaTiempo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nombreArchivo = "reporte_originalidad_" + marcaTiempo + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(contenidoPdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private ResponseEntity<Map<String, Object>> analizarDocumentoInterno(MultipartFile archivo, String textoAnalisis) {

        try {
            String textoExtraido;
            String nombreArchivo;
            String tipoMime;

            if (archivo != null && !archivo.isEmpty()) {
                textoExtraido = extraccionService.extraerTexto(archivo);
                nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename()
                        : "documento-sin-nombre";
                tipoMime = archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream";
            } else if (textoAnalisis != null && !textoAnalisis.isBlank()) {
                textoExtraido = textoAnalisis;
                nombreArchivo = "texto_manual_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
                tipoMime = "text/plain";
            } else {
                Map<String, Object> respuesta = new LinkedHashMap<>();
                respuesta.put("exito", false);
                respuesta.put("mensaje", "Debes subir un archivo o escribir un texto de al menos 70 palabras.");
                return ResponseEntity.badRequest().body(respuesta);
            }

            if (archivo == null && contarPalabras(textoExtraido) < 70) {
                Map<String, Object> respuesta = new LinkedHashMap<>();
                respuesta.put("exito", false);
                respuesta.put("mensaje", "El texto debe tener al menos 70 palabras para ser analizado.");
                return ResponseEntity.badRequest().body(respuesta);
            }

            Documento documentoNuevo = new Documento();
            documentoNuevo.setNombreArchivo(nombreArchivo);
            documentoNuevo.setTipoMime(tipoMime);
            documentoNuevo.setContenidoTexto(textoExtraido);
            documentoNuevo.setUsuario(usuarioPredeterminadoService.obtenerOCrearUsuarioSistema());

            Documento documentoGuardado = documentoRepository.save(documentoNuevo);

            ResultadoComparacion resultado = comparadorSimilitudService.compararTexto(textoExtraido,
                    documentoGuardado.getIdDocumento());

            utp.eidox.model.Analisis analisis = new utp.eidox.model.Analisis();
            analisis.setPorcentaje(resultado.porcentajePlagio());
            analisis.setUsuario(documentoGuardado.getUsuario());
            analisis.setReferenciasEncontradas(SERIALIZADOR_JSON.writeValueAsString(resultado.fuentes()));
            analisisRepository.save(analisis);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("exito", true);
            respuesta.put("porcentajePlagio", resultado.porcentajePlagio());
            respuesta.put("totalPalabras", resultado.totalPalabras());
            respuesta.put("textoOriginal", resultado.textoOriginal());
            respuesta.put("fuentes", resultado.fuentes());

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Error al procesar el documento: " + e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);
        }

    }

    private int contarPalabras(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }

        return texto.trim().split("\\s+").length;
    }

    @SuppressWarnings("unchecked")
    private ResultadoComparacion convertirResultado(Map<String, Object> datos) {
        double porcentaje = datos.get("porcentajePlagio") instanceof Number
                ? ((Number) datos.get("porcentajePlagio")).doubleValue()
                : Double.parseDouble(String.valueOf(datos.get("porcentajePlagio")));

        int totalPalabras = datos.get("totalPalabras") instanceof Number
                ? ((Number) datos.get("totalPalabras")).intValue()
                : Integer.parseInt(String.valueOf(datos.get("totalPalabras")));

        String textoOriginal = datos.get("textoOriginal") != null
                ? String.valueOf(datos.get("textoOriginal"))
                : "";

        List<Map<String, Object>> fuentesMapa = (List<Map<String, Object>>) datos.get("fuentes");
        List<FuenteCoincidencia> fuentes = fuentesMapa == null ? java.util.Collections.emptyList()
                : fuentesMapa.stream().map(this::mapearFuente).toList();

        return new ResultadoComparacion(porcentaje, totalPalabras, textoOriginal, fuentes);
    }

    @SuppressWarnings("unchecked")
    private FuenteCoincidencia mapearFuente(Map<String, Object> fuente) {
        String nombre = fuente.get("nombre") != null ? String.valueOf(fuente.get("nombre")) : "Sin nombre";
        String tipo = fuente.get("tipo") != null ? String.valueOf(fuente.get("tipo")) : "desconocido";
        String url = fuente.get("url") != null ? String.valueOf(fuente.get("url")) : null;
        double porcentaje = fuente.get("porcentaje") instanceof Number
                ? ((Number) fuente.get("porcentaje")).doubleValue()
                : Double.parseDouble(String.valueOf(fuente.getOrDefault("porcentaje", 0)));
        String fragmentoCoincidente = fuente.get("fragmentoCoincidente") != null
                ? String.valueOf(fuente.get("fragmentoCoincidente"))
                : "";
        List<String> fragmentosEnTexto = fuente.get("fragmentosEnTexto") == null ? java.util.Collections.emptyList()
                : ((List<Object>) fuente.get("fragmentosEnTexto")).stream().map(String::valueOf).toList();

        return new FuenteCoincidencia(nombre, tipo, url, porcentaje, fragmentoCoincidente, fragmentosEnTexto);
    }

    private String construirReporteTexto(ResultadoComparacion resultado, String nombreDocumento) {
        String salto = System.lineSeparator();
        StringBuilder reporte = new StringBuilder();

        reporte.append("REPORTE DETALLADO DE ORIGINALIDAD").append(salto);
        reporte.append("================================").append(salto);
        reporte.append("Fecha de generacion: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append(salto);
        reporte.append("Documento: ").append(nombreDocumento).append(salto);
        reporte.append(salto);

        reporte.append("RESUMEN").append(salto);
        reporte.append("-------").append(salto);
        reporte.append("Porcentaje final de similitud: ")
                .append(String.format(java.util.Locale.US, "%.2f", resultado.porcentajePlagio())).append("%")
                .append(salto);
        reporte.append("Contenido original estimado: ")
                .append(String.format(java.util.Locale.US, "%.2f", 100.0 - resultado.porcentajePlagio())).append("%")
                .append(salto);
        reporte.append("Total de palabras analizadas: ").append(resultado.totalPalabras()).append(salto);
        reporte.append("Fuentes detectadas en repositorio local: ").append(resultado.fuentes().size()).append(salto);
        reporte.append(salto);

        reporte.append("FUENTES REFERENCIADAS").append(salto);
        reporte.append("---------------------").append(salto);

        if (resultado.fuentes().isEmpty()) {
            reporte.append("No se encontraron coincidencias en el repositorio local.").append(salto);
        } else {
            for (int i = 0; i < resultado.fuentes().size(); i++) {
                FuenteCoincidencia fuente = resultado.fuentes().get(i);
                reporte.append(i + 1).append(") ").append(fuente.nombre()).append(salto);
                reporte.append("   - Tipo: ").append(fuente.tipo()).append(salto);
                reporte.append("   - Similitud aportada: ")
                        .append(String.format(java.util.Locale.US, "%.2f", fuente.porcentaje())).append("%")
                        .append(salto);
                reporte.append("   - URL/Referencia: ")
                        .append(fuente.url() != null && !fuente.url().isBlank() ? fuente.url() : "Documento interno")
                        .append(salto);
                if (fuente.fragmentoCoincidente() != null && !fuente.fragmentoCoincidente().isBlank()) {
                    reporte.append("   - Fragmento representativo: ").append(fuente.fragmentoCoincidente())
                            .append(salto);
                }
                reporte.append(salto);
            }
        }

        reporte.append("FIN DEL REPORTE").append(salto);
        return reporte.toString();
    }

    private byte[] construirReportePdf(ResultadoComparacion resultado, String nombreDocumento)
            throws DocumentException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, salida);

        Font titulo = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font subtitulo = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font texto = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font textoNegrita = new Font(Font.HELVETICA, 10, Font.BOLD);

        document.open();

        document.add(new Paragraph("REPORTE DETALLADO DE ORIGINALIDAD", titulo));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), texto));
        document.add(new Paragraph("Documento: " + nombreDocumento, texto));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Resumen", subtitulo));
        document.add(new Paragraph("Porcentaje final de similitud: "
                + String.format(java.util.Locale.US, "%.2f", resultado.porcentajePlagio()) + "%", textoNegrita));
        document.add(new Paragraph("Contenido original estimado: "
                + String.format(java.util.Locale.US, "%.2f", 100.0 - resultado.porcentajePlagio()) + "%", texto));
        document.add(new Paragraph("Total de palabras analizadas: " + resultado.totalPalabras(), texto));
        document.add(new Paragraph("Total de fuentes encontradas: " + resultado.fuentes().size(), texto));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Fuentes originales encontradas en el repositorio local", subtitulo));
        document.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(new float[] { 0.7f, 2.4f, 1.2f, 1.2f, 2.5f });
        tabla.setWidthPercentage(100);

        agregarCabeceraTabla(tabla, "#");
        agregarCabeceraTabla(tabla, "Fuente");
        agregarCabeceraTabla(tabla, "Tipo");
        agregarCabeceraTabla(tabla, "% similitud");
        agregarCabeceraTabla(tabla, "Referencia");

        if (resultado.fuentes().isEmpty()) {
            PdfPCell vacio = new PdfPCell(
                    new Phrase("No se encontraron coincidencias en el repositorio local.", texto));
            vacio.setColspan(5);
            vacio.setPadding(6f);
            tabla.addCell(vacio);
        } else {
            for (int i = 0; i < resultado.fuentes().size(); i++) {
                FuenteCoincidencia fuente = resultado.fuentes().get(i);
                tabla.addCell(crearCelda(String.valueOf(i + 1), texto));
                tabla.addCell(crearCelda(safeText(fuente.nombre()), texto));
                tabla.addCell(crearCelda(safeText(fuente.tipo()), texto));
                tabla.addCell(crearCelda(String.format(java.util.Locale.US, "%.2f%%", fuente.porcentaje()), texto));
                tabla.addCell(crearCelda(safeText(fuente.url(), "Documento interno"), texto));
            }
        }

        document.add(tabla);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Fin del reporte", textoNegrita));

        document.close();
        return salida.toByteArray();
    }

    private void agregarCabeceraTabla(PdfPTable tabla, String titulo) {
        Font cabecera = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell celda = new PdfPCell(new Phrase(titulo, cabecera));
        celda.setPadding(6f);
        tabla.addCell(celda);
    }

    private PdfPCell crearCelda(String valor, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, fuente));
        celda.setPadding(6f);
        return celda;
    }

    private String safeText(String valor) {
        return safeText(valor, "-");
    }

    private String safeText(String valor, String fallback) {
        return valor == null || valor.isBlank() ? fallback : valor;
    }

}
