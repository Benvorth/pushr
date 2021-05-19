package de.benvorth.pushr.basicService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="api")
public class PushrController {

    @RequestMapping(
        path = "/is_alive",
        method = RequestMethod.GET
    )
    public String isAlive() {
        return "alive";
    }
}
