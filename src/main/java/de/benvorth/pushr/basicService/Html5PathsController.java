package de.benvorth.pushr.basicService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

// ensure that all page-requests are forwarded to index.html (client-side routing)
// https://stackoverflow.com/questions/44692781/configure-spring-boot-to-redirect-404-to-a-single-page-app
@Controller
public class Html5PathsController {

    // match all paths that do not contain a period and are not already mapped to another controller.
    @RequestMapping(value = "/{[path:[^\\.]*}")
    public String redirect() {
        return "forward:/index.html";
    }
    @RequestMapping(value = "/token/{[path:[^\\.]*}")
    public String redirectToken() {
        return "forward:/index.html";
    }
    @RequestMapping(value = "/trigger/{[path:[^\\.]*}")
    public String redirectTrigger() {
        return "forward:/index.html";
    }
}
