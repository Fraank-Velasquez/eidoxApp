package utp.eidox.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class PlagioService {

    private static final Pattern PATRON_PALABRAS = Pattern.compile("[\\p{L}\\p{N}]+");

    public List<String> normalizarYTokenizar(String textoBruto) {

        if (textoBruto == null || textoBruto.isBlank()) {
            return List.of();
        }

        String textoLimpio = normalizarTexto(textoBruto);

        String[] tokens = textoLimpio.split("\\s+");

        return Arrays.stream(tokens)
                .filter(token -> !token.isBlank())
                .toList();

    }

    public List<String> generarNGramas(List<String> tokens, int tamano) {
        if (tokens == null || tokens.isEmpty() || tamano <= 0 || tokens.size() < tamano) {
            return List.of();
        }

        List<String> nGramas = new ArrayList<>();

        for (int indice = 0; indice <= tokens.size() - tamano; indice++) {
            nGramas.add(String.join(" ", tokens.subList(indice, indice + tamano)));
        }

        return nGramas;
    }

    public List<TokenConPosicion> tokenizarConPosiciones(String textoBruto) {
        if (textoBruto == null || textoBruto.isBlank()) {
            return List.of();
        }

        List<TokenConPosicion> tokens = new ArrayList<>();
        Matcher matcher = PATRON_PALABRAS.matcher(textoBruto);

        while (matcher.find()) {
            String tokenOriginal = matcher.group();
            String tokenNormalizado = normalizarPalabra(tokenOriginal);

            if (!tokenNormalizado.isBlank()) {
                tokens.add(new TokenConPosicion(tokenNormalizado, matcher.start(), matcher.end(), tokenOriginal));
            }
        }

        return tokens;
    }

    public String normalizarPalabra(String palabra) {
        if (palabra == null || palabra.isBlank()) {
            return "";
        }

        String textoLimpio = palabra.toLowerCase();

        textoLimpio = textoLimpio.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o")
                .replace("ú", "u");

        textoLimpio = textoLimpio.replaceAll("[^a-z0-9ñ]", "");

        return textoLimpio.trim();
    }

    public String normalizarTexto(String textoBruto) {
        if (textoBruto == null || textoBruto.isBlank()) {
            return "";
        }

        String textoLimpio = textoBruto.toLowerCase();

        textoLimpio = textoLimpio.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o")
                .replace("ú", "u");

        textoLimpio = textoLimpio.replaceAll("[^a-z0-9ñ\\s]", " ");
        textoLimpio = textoLimpio.replaceAll("\\s+", " ").trim();

        return textoLimpio;
    }

}
