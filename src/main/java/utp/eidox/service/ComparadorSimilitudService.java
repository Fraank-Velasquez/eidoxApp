package utp.eidox.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import utp.eidox.estructura.ArbolAVL;
import utp.eidox.estructura.ReferenciaDocumento;
import utp.eidox.model.Documento;
import utp.eidox.repository.DocumentoRepository;
import utp.eidox.service.dto.FuenteCoincidencia;
import utp.eidox.service.dto.ResultadoComparacion;
import utp.eidox.util.BuscadorBinario;
import utp.eidox.util.OrdenadorQuicksort;

@Service
public class ComparadorSimilitudService {

    private static final int TAMANO_N_GRAMA = 13;

    private final DocumentoRepository documentoRepository;
    private final PlagioService plagioService;
    private final OrdenadorQuicksort ordenadorQuicksort;
    private final BuscadorBinario buscadorBinario;

    public ComparadorSimilitudService(DocumentoRepository documentoRepository, PlagioService plagioService) {
        this.documentoRepository = documentoRepository;
        this.plagioService = plagioService;
        this.ordenadorQuicksort = new OrdenadorQuicksort();
        this.buscadorBinario = new BuscadorBinario();
    }

    public ResultadoComparacion compararTexto(String textoOriginal, Long idDocumentoExcluido) {

        List<TokenConPosicion> tokensNuevoDocumento = plagioService.tokenizarConPosiciones(textoOriginal);
        List<String> tokensNormalizadosNuevoDocumento = tokensNuevoDocumento.stream()
                .map(token -> token.getTokenNormalizado())
                .toList();
        List<String> nGramasNuevoDocumento = ordenadorQuicksort
                .ordenar(plagioService.generarNGramas(tokensNormalizadosNuevoDocumento, TAMANO_N_GRAMA));

        if (nGramasNuevoDocumento.isEmpty()) {
            return new ResultadoComparacion(0.0, tokensNuevoDocumento.size(), textoOriginal, List.of());
        }

        ArbolAVL indiceDocumento = construirIndiceRepositorio(idDocumentoExcluido);
        Map<Long, AcumuladoFuente> fuentesEncontradas = new LinkedHashMap<>();
        int coincidenciasGlobales = 0;

        for (String nGramaNormalizado : nGramasNuevoDocumento) {
            List<ReferenciaDocumento> referencias = indiceDocumento.buscar(nGramaNormalizado);

            if (referencias.isEmpty()) {
                continue;
            }

            coincidenciasGlobales++;
            String fragmentoExacto = extraerFragmentoExacto(tokensNuevoDocumento, textoOriginal, nGramaNormalizado);

            for (ReferenciaDocumento referencia : referencias) {
                AcumuladoFuente acumulado = fuentesEncontradas.computeIfAbsent(referencia.getIdDocumento(),
                        clave -> new AcumuladoFuente(referencia.getNombreDocumento(), referencia.getTipoDocumento()));

                acumulado.coincidencias++;
                if (acumulado.fragmentoCoincidente == null) {
                    acumulado.fragmentoCoincidente = fragmentoExacto;
                }

                if (fragmentoExacto != null && !fragmentoExacto.isBlank()) {
                    acumulado.fragmentos.add(fragmentoExacto);
                }
            }
        }

        double porcentajePlagio = redondear((coincidenciasGlobales * 100.0) / nGramasNuevoDocumento.size());
        List<FuenteCoincidencia> fuentes = construirFuentes(fuentesEncontradas, nGramasNuevoDocumento.size());

        return new ResultadoComparacion(porcentajePlagio, tokensNuevoDocumento.size(), textoOriginal, fuentes);
    }

    private ArbolAVL construirIndiceRepositorio(Long idDocumentoExcluido) {
        ArbolAVL indice = new ArbolAVL();

        for (Documento documento : documentoRepository.findAll()) {
            if (idDocumentoExcluido != null && idDocumentoExcluido.equals(documento.getIdDocumento())) {
                continue;
            }

            if (documento.getContenidoTexto() == null || documento.getContenidoTexto().isBlank()) {
                continue;
            }

            List<TokenConPosicion> tokensDocumento = plagioService
                    .tokenizarConPosiciones(documento.getContenidoTexto());
            List<String> tokensNormalizados = tokensDocumento.stream().map(token -> token.getTokenNormalizado()).toList();
            List<String> nGramasOrdenados = ordenadorQuicksort
                    .ordenar(plagioService.generarNGramas(tokensNormalizados, TAMANO_N_GRAMA));

            if (nGramasOrdenados.isEmpty()) {
                continue;
            }

            Set<String> nGramasUnicos = new LinkedHashSet<>(nGramasOrdenados);

            for (String nGramaNormalizado : nGramasUnicos) {
                String fragmentoExacto = extraerFragmentoExacto(tokensDocumento, documento.getContenidoTexto(),
                        nGramaNormalizado);
                if (fragmentoExacto == null || fragmentoExacto.isBlank()) {
                    continue;
                }

                indice.insertar(nGramaNormalizado, new ReferenciaDocumento(documento.getIdDocumento(),
                        documento.getNombreArchivo(), inferirTipoDocumento(documento), fragmentoExacto));
            }

            if (!nGramasUnicos.isEmpty()) {
                buscadorBinario.buscarIndice(nGramasOrdenados, nGramasUnicos.iterator().next());
            }
        }

        return indice;
    }

    private List<FuenteCoincidencia> construirFuentes(Map<Long, AcumuladoFuente> fuentesEncontradas,
            int totalNGramasDocumentoNuevo) {

        List<FuenteCoincidencia> fuentes = new ArrayList<>();

        for (AcumuladoFuente acumulado : fuentesEncontradas.values()) {
            double porcentaje = redondear((acumulado.coincidencias * 100.0) / totalNGramasDocumentoNuevo);
            List<String> fragmentos = new ArrayList<>(acumulado.fragmentos);

            fuentes.add(new FuenteCoincidencia(acumulado.nombreDocumento, acumulado.tipoDocumento, null, porcentaje,
                    acumulado.fragmentoCoincidente, fragmentos));
        }

        return fuentes;
    }

    private String extraerFragmentoExacto(List<TokenConPosicion> tokens, String textoOriginalCompleto,
            String nGramaNormalizado) {
        if (tokens.isEmpty() || nGramaNormalizado == null || nGramaNormalizado.isBlank()) {
            return null;
        }

        String[] partes = nGramaNormalizado.split("\\s+");

        if (partes.length == 0 || tokens.size() < partes.length) {
            return null;
        }

        for (int indice = 0; indice <= tokens.size() - partes.length; indice++) {
            boolean coincide = true;

            for (int offset = 0; offset < partes.length; offset++) {
                if (!tokens.get(indice + offset).getTokenNormalizado().equals(partes[offset])) {
                    coincide = false;
                    break;
                }
            }

            if (coincide) {
                int inicio = tokens.get(indice).getInicio();
                int fin = tokens.get(indice + partes.length - 1).getFin();
                return textoOriginalCompleto.substring(inicio, fin).trim();
            }
        }

        return null;
    }

    private String inferirTipoDocumento(Documento documento) {
        String tipoMime = documento.getTipoMime() == null ? "" : documento.getTipoMime().toLowerCase();
        String nombreArchivo = documento.getNombreArchivo() == null ? "" : documento.getNombreArchivo().toLowerCase();

        if (tipoMime.contains("pdf") || nombreArchivo.endsWith(".pdf")) {
            return "pdf";
        }

        return "word";
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static class AcumuladoFuente {

        private final String nombreDocumento;
        private final String tipoDocumento;
        private int coincidencias;
        private String fragmentoCoincidente;
        private final Set<String> fragmentos = new LinkedHashSet<>();

        private AcumuladoFuente(String nombreDocumento, String tipoDocumento) {
            this.nombreDocumento = nombreDocumento;
            this.tipoDocumento = tipoDocumento;
        }
    }
}
