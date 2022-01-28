package it.cnr.iasi.saks.inspection.test.sampleApplication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleController {

    @GetMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

}
