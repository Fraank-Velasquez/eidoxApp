package utp.eidox.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import utp.eidox.model.Analisis;
import utp.eidox.model.Documento;
import utp.eidox.repository.AnalisisRepository;
import utp.eidox.repository.DocumentoRepository;
import utp.eidox.service.dto.FuenteCoincidencia;
import utp.eidox.service.dto.ResultadoComparacion;

@Service
@RequiredArgsConstructor
public class AnalisisService {

    private static final ObjectMapper SERIALIZADOR_JSON = new ObjectMapper();

    private final ExtraccionTikaService extraccionService;
    private final ComparadorSimilitudService comparadorSimilitudService;
    private final DocumentoRepository documentoRepository;
    private final AnalisisRepository analisisRepository;
    private final UsuarioPredeterminadoService usuarioPredeterminadoService;

    public Map<String, Object> procesarAnalisis(MultipartFile archivo, String textoAnalisis) throws Exception {
        String textoExtraido;
        String nombreArchivo;
        String tipoMime;

        // Extraer y validar origen del contenido
        if (archivo != null && !archivo.isEmpty()) {
            textoExtraido = extraccionService.extraerTexto(archivo);
            nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename()
                    : "documento-sin-nombre";
            tipoMime = archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream";
        } else if (textoAnalisis != null && !textoAnalisis.isBlank()) {
            textoExtraido = textoAnalisis;
            nombreArchivo = "texto_manual_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".txt";
            tipoMime = "text/plain";
        } else {
            throw new IllegalArgumentException("Debes subir un archivo o escribir un texto de al menos 70 palabras.");
        }

        if (archivo == null && contarPalabras(textoExtraido) < 70) {
            throw new IllegalArgumentException("El texto debe tener al menos 70 palabras para ser analizado.");
        }

        //  Verificar si el documento ya existe en el

        Optional<Documento> documentoExistente = documentoRepository.findByNombreArchivo(nombreArchivo);
        Documento documentoGuardado;
        boolean esDocumentoNuevo = documentoExistente.isEmpty();

        if (documentoExistente.isPresent()) {
            // Si el documento ya existe, NO lo guardamos de nuevo. Reutilizamos el registro existente.
            documentoGuardado = documentoExistente.get();
        } else {
            // Si el documento es completamente nuevo, procedemos a registrarlo en la BD
            Documento documentoNuevo = new Documento();
            documentoNuevo.setNombreArchivo(nombreArchivo);
            documentoNuevo.setTipoMime(tipoMime);
            documentoNuevo.setContenidoTexto(textoExtraido);
            documentoNuevo.setUsuario(usuarioPredeterminadoService.obtenerOCrearUsuarioSistema());

            documentoGuardado = documentoRepository.save(documentoNuevo);
        }

        
        Long idAExcluir = esDocumentoNuevo ? documentoGuardado.getIdDocumento() : null;
        ResultadoComparacion resultado = comparadorSimilitudService.compararTexto(textoExtraido, idAExcluir);
        //  Guardar bitácora del análisis en la BD
        Analisis analisis = new Analisis();
        analisis.setPorcentaje(resultado.getPorcentajePlagio());
        analisis.setUsuario(documentoGuardado.getUsuario());
        analisis.setReferenciasEncontradas(SERIALIZADOR_JSON.writeValueAsString(resultado.getFuentes()));
        analisisRepository.save(analisis);

        // Estructurar respuesta para el controlador
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("exito", true);
        respuesta.put("porcentajePlagio", resultado.getPorcentajePlagio());
        respuesta.put("totalPalabras", resultado.getTotalPalabras());
        respuesta.put("textoOriginal", resultado.getTextoOriginal());
        respuesta.put("fuentes", resultado.getFuentes());

        return respuesta;
    }

    public byte[] generarReportePdf(Map<String, Object> datosAnalisis) throws DocumentException {
        ResultadoComparacion resultado = convertirResultado(datosAnalisis);
        String nombreDocumento = datosAnalisis.get("nombreDocumento") != null
                ? String.valueOf(datosAnalisis.get("nombreDocumento"))
                : "Documento analizado";
        return construirReportePdf(resultado, nombreDocumento);
    }

    private int contarPalabras(String texto) {
        if (texto == null || texto.isBlank())
            return 0;
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
        String textoOriginal = datos.get("textoOriginal") != null ? String.valueOf(datos.get("textoOriginal")) : "";
        List<Map<String, Object>> fuentesMapa = (List<Map<String, Object>>) datos.get("fuentes");
        List<FuenteCoincidencia> fuentes = fuentesMapa == null ? Collections.emptyList()
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
        List<String> fragmentosEnTexto = fuente.get("fragmentosEnTexto") == null ? Collections.emptyList()
                : ((List<Object>) fuente.get("fragmentosEnTexto")).stream().map(String::valueOf).toList();
        return new FuenteCoincidencia(nombre, tipo, url, porcentaje, fragmentoCoincidente, fragmentosEnTexto);
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
        document.add(new Paragraph("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tREPORTE DETALLADO DE ORIGINALIDAD", titulo));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), texto));
        document.add(new Paragraph("Documento: " + nombreDocumento, texto));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Resumen", subtitulo));
        document.add(new Paragraph("Porcentaje final de similitud: "
                + String.format(Locale.US, "%.2f", resultado.getPorcentajePlagio()) + "%", textoNegrita));
        document.add(new Paragraph("Contenido original estimado: "
                + String.format(Locale.US, "%.2f", 100.0 - resultado.getPorcentajePlagio()) + "%", texto));
        document.add(new Paragraph("Total de palabras analizadas: " + resultado.getTotalPalabras(), texto));
        document.add(new Paragraph("Total de fuentes encontradas: " + resultado.getFuentes().size(), texto));
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

        if (resultado.getFuentes().isEmpty()) {
            PdfPCell vacio = new PdfPCell(
                    new Phrase("No se encontraron coincidencias en el repositorio local.", texto));
            vacio.setColspan(5);
            vacio.setPadding(6f);
            tabla.addCell(vacio);
        } else {
            for (int i = 0; i < resultado.getFuentes().size(); i++) {
                FuenteCoincidencia fuente = resultado.getFuentes().get(i);
                tabla.addCell(crearCelda(String.valueOf(i + 1), texto));
                tabla.addCell(crearCelda(safeText(fuente.getNombre()), texto));
                tabla.addCell(crearCelda(safeText(fuente.getTipo()), texto));
                tabla.addCell(crearCelda(String.format(Locale.US, "%.2f%%", fuente.getPorcentaje()), texto));
                tabla.addCell(crearCelda(safeText(fuente.getUrl(), "Documento interno"), texto));
            }
        }
        document.add(tabla);
        for (int i = 0; i < 5; i++) {
            document.add(new Paragraph(" "));
        }
        document.add(new Paragraph("Fin del reporte \t ©  Eidox 2026", textoNegrita));
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