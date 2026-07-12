package utp.eidox.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import utp.eidox.repository.AnalisisRepository;

@Controller
@RequiredArgsConstructor
public class homeController {

    private final AnalisisRepository analisisRepository;

    @GetMapping({ "/", "/inicio" })
    public String inicio(Model model) {
        long totalAnalisis = analisisRepository.count();
        model.addAttribute("totalDocumentosAnalizados", totalAnalisis);
        return "pages/inicio";
    }

    @GetMapping("/analisis")
    public String analisis() {
        return "pages/analisis";
    }

}