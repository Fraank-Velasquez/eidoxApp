package utp.eidox.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class homeController {

    @GetMapping({ "/", "/inicio" })
    public String inicio() {
        return "pages/inicio";
    }

    @GetMapping("/analisis")
    public String analisis() {
        return "pages/analisis";
    }

}