package utp.eidox.service;

import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExtraccionTikaService {

    private final Tika analizadorTika = new Tika();

    public String extraerTexto(MultipartFile archivo) throws Exception {
        try (InputStream flujoEntrada = archivo.getInputStream()) {
            return analizadorTika.parseToString(flujoEntrada);
        } catch (TikaException e) {
            throw new RuntimeException(e);
        }
    }
}
