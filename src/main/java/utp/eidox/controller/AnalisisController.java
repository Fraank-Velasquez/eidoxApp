package utp.eidox.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import utp.eidox.service.AnalisisService;

@RestController
@RequestMapping("/api/analisis")
@RequiredArgsConstructor
public class AnalisisController {

    private final AnalisisService analisisService;

    @PostMapping("/subir")
    public ResponseEntity<Map<String, Object>> analizarDocumento(@RequestParam("archivo") MultipartFile archivo) {
        return ejecutarAnalisisInterno(archivo, null);
    }

    @PostMapping(value = "/subir", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, Object>> analizarTexto(
            @RequestParam(value = "textoAnalisis", required = false) String textoAnalisis) {
        return ejecutarAnalisisInterno(null, textoAnalisis);
    }

    @PostMapping(value = "/subir", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> analizarMixto(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "textoAnalisis", required = false) String textoAnalisis) {
        return ejecutarAnalisisInterno(archivo, textoAnalisis);
    }

    private ResponseEntity<Map<String, Object>> ejecutarAnalisisInterno(MultipartFile archivo, String textoAnalisis) {
        try {
            Map<String, Object> respuesta = analisisService.procesarAnalisis(archivo, textoAnalisis);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            // Captura errores controlados (menos de 70 palabras, sin datos de entrada, etc.)
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("exito", false);
            respuesta.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);
        } catch (Exception e) {
            // Captura cualquier otro error técnico inesperado
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Error al procesar el documento: " + e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);
        }
    }

    @PostMapping(value = "/reporte/descargar-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> descargarReportePdf(@RequestBody Map<String, Object> resultadoAnalisis) {
        try {
            byte[] contenidoPdf = analisisService.generarReportePdf(resultadoAnalisis);
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
}